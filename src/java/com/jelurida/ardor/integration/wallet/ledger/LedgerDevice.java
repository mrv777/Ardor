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

import nxt.env.RuntimeEnvironment;
import nxt.util.Logger;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

public abstract class LedgerDevice {

    private static final int VENDOR_LEDGER = 0x2c97;
    private static final int INTERFACE_NUMBER = 0;
    private static final int USAGE_PAGE_LEDGER = 0xffffffa0;

    private static LedgerDevice findLedgerDeviceHIDAPI() {
        return PureJavaHidApi.enumerateDevices().stream()
                .filter(LedgerDevice::isLedger).findFirst().map(LedgerHIDAPI::new)
                .orElse(null);
    }

    public static LedgerDevice findLedgerDevice() {
        return findLedgerDeviceHIDAPI();
    }

    private static boolean isLedger(HidDeviceInfo devInfo) {
        Logger.logInfoMessage("Device info - VID: 0x%04X, PID: 0x%04X, product: %s, usage page: 0x%04X, path: %s",
                devInfo.getVendorId(), devInfo.getProductId(), devInfo.getProductString(), devInfo.getUsagePage(),
                devInfo.getPath());
        if (devInfo.getVendorId() != VENDOR_LEDGER) {
            return false;
        }

        if (RuntimeEnvironment.isWindowsRuntime()) {
            return devInfo.getUsagePage() == USAGE_PAGE_LEDGER;
        }

        return true; // seems we can't do any more filtering
    }

    public abstract byte[] exchange(byte[] input);
}
