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

package nxt.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bip32Path implements Cloneable {

    private static final int HARDENED = 0x80000000;
    private static final Pattern PATH_TOKEN = Pattern.compile("(\\d+)([hH']?)");

    private final int[] path; // keep this array immutable, never allow code to change its elements

    private Bip32Path(int[] path) {
        this.path = path;
    }

    public static byte[] bip32PathToBytes(int[] path) {
        return toByteArray(path, ByteOrder.LITTLE_ENDIAN);
    }

    public static String bip32PathToStr(int[] path) {
        return toString(path, false, false);
    }

    public static int[] bip32StrToPath(String pathStr) {
        return fromString(pathStr, false);
    }

    public int[] toPathArray() {
        return Arrays.copyOf(path, path.length); // always clone the internal path before returning it
    }

    public byte[] toByteArray(ByteOrder byteOrder) {
        return toByteArray(path, byteOrder);
    }

    public static byte[] toByteArray(int[] path, ByteOrder byteOrder) {
        ByteBuffer pathBytes = ByteBuffer.allocate(4 * path.length);
        pathBytes.order(byteOrder);
        for (int value : path) {
            pathBytes.putInt(value);
        }
        return pathBytes.array();
    }

    public static boolean validateString(String text) {
        return validateString(text, true);
    }

    public static boolean validateString(String text, boolean reqRoot) {
        try {
            fromString(text, reqRoot);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Bip32Path fromString(String text) {
        return new Bip32Path(fromString(text, true));
    }

    public static int[] fromString(String text, boolean reqRoot) {
        text = text.toLowerCase();
        if (text.startsWith("m/")) {
            text = text.substring(2);
        } else if (reqRoot) {
            throw new IllegalArgumentException("Path " + text + " has no root");
        }

        String[] path = text.split("/");
        int[] ret = new int[path.length];
        for (int i = 0; i < path.length; i++) {
            Matcher matcher = PATH_TOKEN.matcher(path[i]);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid path token: " + path[i]);
            }
            String index = matcher.group(1);
            ret[i] = Integer.parseInt(index);
            String modifier = matcher.group(2);
            if ("".equals(modifier)) {
                continue;
            }
            if ("h".equals(modifier) || "H".equals(modifier) || "'".equals(modifier)) {
                ret[i] += HARDENED;
            } else {
                throw new IllegalArgumentException("Invalid modifier: " + modifier);
            }
        }
        return ret;
    }

    public static String toString(int[] path, boolean noRoot, boolean oldStyle) {
        String[] ret = new String[path.length];
        for (int i = 0; i < path.length; i++) {
            int token = path[i];
            if ((token & HARDENED) != 0) {
                ret[i] = (token & ~HARDENED) + (oldStyle ? "h" : "'");
            } else {
                ret[i] = Integer.toString(token);
            }
        }
        return (noRoot ? "" : "m/") + String.join("/", ret);
    }

    public Bip32Path appendChild(int index) {
        int[] newPath = Arrays.copyOf(path, path.length + 1);
        newPath[path.length] = index;
        return new Bip32Path(newPath);
    }

    public Bip32Path updateLastChild(int index) {
        int[] newPath = Arrays.copyOf(path, path.length);
        newPath[path.length - 1] = index;
        return new Bip32Path(newPath);
    }

    @Override
    public String toString() {
        return toString(path, false, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bip32Path bip32Path = (Bip32Path) o;
        return Arrays.equals(path, bip32Path.path);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(path);
    }
}
