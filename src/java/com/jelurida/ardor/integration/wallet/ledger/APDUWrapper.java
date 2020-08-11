/*
 * Copyright (c) 2018 Aion Network
 * Ledger4j https://github.com/aionnetwork/ledger4j is licensed under the MIT license:
 * https://github.com/aionnetwork/ledger4j/blob/master/LICENSE
 *
 */

package com.jelurida.ardor.integration.wallet.ledger;

import java.nio.ByteBuffer;

import static com.jelurida.ardor.integration.wallet.ledger.ByteUtilities.trimHead;
import static com.jelurida.ardor.integration.wallet.ledger.ByteUtilities.trimTail;

public class APDUWrapper {

    public static class SerializedPacket {
        public byte[] data;
        public int offset;

        public SerializedPacket(byte[] serialized, int offset) {
            this.data = serialized;
            this.offset = offset;
        }
    }

    public static class DeserializedPacket {
        public byte[] data;

        // total response length of all command data this will only be a non-zero value in the first packet
        public int totalResponseLength;

        public DeserializedPacket(byte[] data, int totalResponseLength) {
            this.data = data;
            this.totalResponseLength = totalResponseLength;
        }
    }

    public static class DeserializationException extends Exception {
        public DeserializationException(String reason) {
            super(reason);
        }
    }

    static SerializedPacket serializePacket(int channel, byte[] command, int commandOffset, int sequenceIdx, boolean ble) {
        if (channel <= 0) {
            throw new IllegalArgumentException("channel must be > 0");
        }

        if (channel > (2 << 16)) {
            throw new IllegalArgumentException("channel must be <= (2 << 16)");
        }

        if (sequenceIdx > (2 << 16)) {
            throw new IllegalArgumentException("sequenceIdx must be <= (2 << 16)");
        }

        // allocate into 64 byte chunks
        final ByteBuffer buf = ByteBuffer.allocate(LedgerHIDAPI.PACKET_SIZE);
        if (!ble) {
            buf.putShort((short) channel);
        }

        buf.put((byte) 0x05);
        buf.putShort((short) sequenceIdx);

        if (sequenceIdx == 0) {
            buf.putShort((short) command.length);
        }

        // remaining capacity is attributed to command data
        final int capacity = Math.min(buf.remaining(), command.length - commandOffset);
        final byte[] cmdArr = new byte[capacity];
        System.arraycopy(command, commandOffset, cmdArr, 0, capacity);
        buf.put(cmdArr);

        final byte[] bufArr = buf.array();
        return new SerializedPacket(bufArr, capacity);
    }

    public static byte[] wrapCommandAPDU(int channel, byte[] command, boolean ble) {
        int commandOffset = 0;
        int sequenceIdx = 0;
        byte[] outBuf = new byte[0];

        while (command.length - commandOffset > 0) {
            SerializedPacket packet = serializePacket(channel, command, commandOffset, sequenceIdx, ble);

            // TODO: optimize later
            outBuf = ByteBuffer.allocate(outBuf.length + packet.data.length).put(outBuf).put(packet.data).array();

            commandOffset += packet.offset;
            sequenceIdx++;
        }
        return outBuf;
    }

    public static DeserializedPacket deserializePacket(int channel, byte[] buffer, int sequenceIdx, boolean ble) throws DeserializationException {
        if ((sequenceIdx == 0 && buffer.length < 7) || (sequenceIdx > 0 && buffer.length < 5)) {
            // TODO: more meaningful exception
            throw new DeserializationException("Cannot deserialize packet, header information missing");
        }

        final int offset = sequenceIdx * LedgerHIDAPI.PACKET_SIZE;
        // TODO: this can be optimized, offsets anyone?
        byte[] trimBuf = trimHead(buffer, offset);
        trimBuf = trimTail(trimBuf, trimBuf.length - LedgerHIDAPI.PACKET_SIZE);
        ByteBuffer buf = ByteBuffer.wrap(trimBuf);
        if (!ble) {
            final int dChannel = buf.getShort() & 0x0000FFFF;
            if (dChannel != channel) {
                throw new DeserializationException("Invalid channel");
            }
        }

        if (buf.get() != 0x05) {
            throw new DeserializationException("Invalid tag");
        }

        final int dSequenceIdx = buf.getShort() & 0x0000FFFF;
        if (dSequenceIdx != sequenceIdx) {
            throw new DeserializationException("Invalid sequenceIdx");
        }

        int totalResponseLength = 0;
        if (sequenceIdx == 0) {
            totalResponseLength = buf.getShort() & 0x0000FFFF;
        }

        final byte[] payload = new byte[buf.remaining()];
        buf.get(payload);
        return new DeserializedPacket(payload, totalResponseLength);
    }

    public static byte[] unwrapResponseAPDU(int channel, byte[] data, boolean ble) throws DeserializationException {
        int sequenceIdx = 0;
        int totalResponseLength = 0;
        ByteBuffer outBuffer = null;

        while (true) {
            int extraHeaderSize = 0;

            // the first packet always contains extra information
            if (sequenceIdx == 0) {
                extraHeaderSize = 2;
            }

            DeserializedPacket packet = deserializePacket(channel, data, sequenceIdx, ble);

            // this is always guaranteed to run on the first loop
            if (sequenceIdx == 0) {
                totalResponseLength = packet.totalResponseLength;
                outBuffer = ByteBuffer.allocate(totalResponseLength);
            }

            if (5 + extraHeaderSize + totalResponseLength > data.length) {
                return null;
            }

            byte[] deserializedPayload = packet.data;
            if (outBuffer.remaining() < deserializedPayload.length) {
                deserializedPayload = trimTail(deserializedPayload, deserializedPayload.length - outBuffer.remaining());
            }
            outBuffer.put(deserializedPayload);

            if (!outBuffer.hasRemaining()) {
                break;
            }
            sequenceIdx++;
        }
        return outBuffer.array();
    }
}
