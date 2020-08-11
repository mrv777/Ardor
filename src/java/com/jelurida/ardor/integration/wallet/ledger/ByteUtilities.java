/*
 * Copyright (c) 2018 Aion Network
 * Ledger4j https://github.com/aionnetwork/ledger4j is licensed under the MIT license:
 * https://github.com/aionnetwork/ledger4j/blob/master/LICENSE
 *
 */

package com.jelurida.ardor.integration.wallet.ledger;

public class ByteUtilities {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static byte[] merge(byte[] in1, byte[] in2) {
        byte[] out = new byte[in1.length + in2.length];
        System.arraycopy(in1, 0, out, 0, in1.length);
        System.arraycopy(in2, 0, out, in1.length, in2.length);
        return out;
    }

    public static byte[] trimHead(byte[] in, int amount) {
        if (amount >= in.length) {
            return new byte[0];
        }

        byte[] out = new byte[in.length - amount];
        System.arraycopy(in, amount, out, 0, in.length - amount);
        return out;
    }

    public static byte[] trimTail(byte[] in, int amount) {
        if (amount >= in.length) {
            return new byte[0];
        }

        byte[] out = new byte[in.length - amount];
        System.arraycopy(in, 0, out, 0, out.length);
        return out;
    }

    public static String shortToHex(short amount) {
        return "0x" + bytesToHex(toByteArray(amount));
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of characters.");
        }
        if (hex.substring(0, 2).equals("0x")) {
            hex = hex.substring(2);
        }
        return hexToBytes(hex.toCharArray());
    }

    private static byte[] hexToBytes(char[] hex) {
        if (hex.length % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of characters.");
        }

        int length = hex.length >> 1;
        byte[] raw = new byte[length];
        for (int o = 0, i = 0; o < length; o++) {
            raw[o] = (byte)((getHexCharValue(hex[i++]) << 4) | getHexCharValue(hex[i++]));
        }
        return raw;
    }

    private static byte getHexCharValue(char c) {
        if (c >= '0' && c <= '9') {
            return (byte) (c - '0');
        }

        if (c >= 'A' && c <= 'F') {
            return (byte) (10 + c - 'A');
        }

        if (c >= 'a' && c <= 'f') {
            return (byte) (10 + c - 'a');
        }
        throw new IllegalArgumentException("Invalid hex character");
    }

    /**
     * Returns a byte array given an int, function is guaranteed to return a non-null value that is of length 4
     *
     * @param i input integer
     * @return big-endian encoded byte array of length 4
     */

    public static byte[] toByteArray(int i) {
        byte[] val = new byte[4];
        val[3] = (byte) (i & 0xFF);
        val[2] = (byte) ((i >> 8) & 0xFF);
        val[1] = (byte) ((i >> 16) & 0xFF);
        val[0] = (byte) ((i >> 24) & 0xFF);
        return val;
    }

    private static byte[] toByteArray(short i) {
        byte[] val = new byte[2];
        val[1] = (byte) (i & 0xFF);
        val[0] = (byte) ((i >> 8) & 0xFF);
        return val;
    }

    @SuppressWarnings("unused")
    public static byte[] toHardenedOffset(int i) {
        byte[] offset = toByteArray(i);
        offset[0] = (byte) (offset[0] | (byte) 0x80);
        return offset;
    }
}
