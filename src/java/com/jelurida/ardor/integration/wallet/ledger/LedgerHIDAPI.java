/*
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.jelurida.ardor.integration.wallet.ledger;

import nxt.util.Logger;
import purejavahidapi.DeviceRemovalListener;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Ledger device connected via USB HID.
 *
 * This class is not thread safe.
 */
public class LedgerHIDAPI extends LedgerDevice implements InputReportListener, DeviceRemovalListener {
    static final int PACKET_SIZE = 64;
    private static final int CHANNEL = 0x0101;

    private final HidDeviceInfo hidDeviceInfo;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private HidDevice device;
    private byte[] ledgerResponse;
    private byte[] responseAPDU;

    LedgerHIDAPI(HidDeviceInfo hidDeviceInfo) {
        this.hidDeviceInfo = hidDeviceInfo;
    }

    private synchronized void open() {
        if (device == null) {
            try {
                device = PureJavaHidApi.openDevice(hidDeviceInfo);
                device.setInputReportListener(this);
                device.setDeviceRemovalListener(this);
            } catch (IOException e) {
                throw new RuntimeException("Cannot open ledger device", e);
            }
        }
    }

    private synchronized void close(boolean closeDevice) {
        if (device != null) {
            device.setDeviceRemovalListener(null);
            device.setInputReportListener(null);
            if (closeDevice) {
                // Send fake output report so close won't lock
                device.setOutputReport((byte) 0, new byte[64], 64);
                device.close();
            }
            device = null;
        }
    }

    @Override
    public byte[] exchange(byte[] input) {
        open();
        lock.lock();
        try {
            byte[] wrappedInput = APDUWrapper.wrapCommandAPDU(CHANNEL, input, false);
            ByteBuffer buffer = ByteBuffer.wrap(wrappedInput);
            while (buffer.remaining() >= PACKET_SIZE) {
                final byte[] packet = new byte[PACKET_SIZE];
                buffer.get(packet);
                int ret = device.setOutputReport((byte) 0, packet, PACKET_SIZE);
                if (ret == -1) {
                    close(true);
                    return null;
                }
            }
            ledgerResponse = new byte[0];
            responseAPDU = null;
            condition.await();

            // interpret results of deserialization
            if (responseAPDU == null || responseAPDU.length < 2) {
                close(true);
                return null;
            }
            final int swOffset = responseAPDU.length - 2;
            final int sw = (responseAPDU[swOffset] << 8 + responseAPDU[swOffset + 1]) & 0xFFFF;
            if (sw != 0x9000) {
                throw new IllegalStateException(String.format("Device error code: 0x%04X", sw));
            }
            return ByteUtilities.trimTail(responseAPDU, 2);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Ledger exchange interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onInputReport(HidDevice hidDevice, byte b, byte[] data, int length) {
        lock.lock();
        try {
            byte[] respPacket = Arrays.copyOfRange(data, 0, length);
            ledgerResponse = ByteUtilities.merge(ledgerResponse, respPacket);
            byte[] deserialized = APDUWrapper.unwrapResponseAPDU(CHANNEL, ledgerResponse, false);
            if (deserialized != null) {
                responseAPDU = deserialized;
                condition.signal();
            }
        } catch (Exception e) {
            Logger.logInfoMessage("Exception reading Ledger response", e);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onDeviceRemoval(HidDevice hidDevice) {
        lock.lock();
        try {
            Logger.logInfoMessage("Ledger device removed.");
            close(false);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}
