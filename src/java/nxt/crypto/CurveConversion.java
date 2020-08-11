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
package nxt.crypto;

import nxt.util.Convert;

/**
 * Montgomery &lt;-&gt; Edwards isomorphism converted from C code developed by Daniel Beer (dlbeer@gmail.com) and released as public domain.
 * Since the original code uses unsigned integer we use short data type instead of byte for calculations.
 **/
public class CurveConversion {
    private static final int SIZE = 32;
    private static final short[] ONE = new short[SIZE];

    static {
        ONE[0] = (short) 1;
    }

    private static void select(short[] dst, short[] zero, short[] one, short condition) {
        short mask = (short)-condition;
        for (int i = 0; i < SIZE; i++) {
            dst[i] = (short) (zero[i] ^ (mask & (one[i] ^ zero[i])));
        }
    }

    private static void normalize(short[] x) {
        /* Reduce using 2^255 = 19 mod p */
        int c = (x[31] >> 7) * 19;
        x[31] &= 0x7f;

        int i;
        for (i = 0; i < SIZE; i++) {
            c += x[i];
            x[i] = (short)(c & 0xff);
            c >>= 8;
        }

        /* The number is now less than 2^255 + 18, and therefore less than 2p.
         * Try subtracting p, and conditionally load the subtracted value if underflow did not occur. */
        c = 19;

        short[] minusp = new short[SIZE];
        for (i = 0; i + 1 < SIZE; i++) {
            c += x[i];
            minusp[i] = (short)(c & 0xff);
            c >>= 8;
        }

        c += ((int) x[i]) - 0x80;
        minusp[31] = (short)(c & 0xff);

        /* Load x-p if no underflow */
        select(x, minusp, x, (short) ((c >> 15) & 1));
    }

    private static void add(short[] r, short[] b) {
        /* Add */
        int c = 0;
        for (int i = 0; i < SIZE; i++) {
            c >>= 8;
            c += ((int) ONE[i]) + ((int) b[i]);
            r[i] = (short)(c & 0xff);
        }

        /* Reduce with 2^255 = 19 mod p */
        r[31] &= 127;
        c = (c >> 7) * 19;

        for (int i = 0; i < SIZE; i++) {
            c += r[i];
            r[i] = (short)(c & 0xff);
            c >>= 8;
        }
    }

    private static void sub(short[] r, short[] b) {
        /* Calculate a + 2p - b, to avoid underflow */
        long c = 218;
        for (int i = 0; i + 1 < SIZE; i++) {
            c += 65280 + ((long) ONE[i]) - ((long) b[i]);
            r[i] = (short) (c & 0xff);
            c >>= 8;
        }

        c += ((long) ONE[31]) - ((long) b[31]);
        r[31] = (short) (c & 0x7f);
        c = (c >> 7) * 19;

        for (int i = 0; i < SIZE; i++) {
            c += r[i];
            r[i] = (short) (c & 0xff);
            c >>= 8;
        }
    }

    private static void mulDistinct(short[] r, short[] a, short[] b) {
        long c = 0;
        for (int i = 0; i < SIZE; i++) {
            int j;
            c >>= 8;
            for (j = 0; j <= i; j++) {
                c += (a[j] & 0xff) * (b[i - j] & 0xff);
            }

            for (; j < SIZE; j++) {
                c += (a[j] & 0xff) * (b[i + SIZE - j] & 0xff) * 38;
            }

            r[i] = (short) (c & 0xff);
        }

        r[31] &= 127;
        c = (c >> 7) * 19;

        for (int i = 0; i < SIZE; i++) {
            c += r[i];
            r[i] = (short) (c & 0xff);
            c >>= 8;
        }
    }

    /**
     *  This is a prime field, so by Fermat's little theorem: x^(p-1) = 1 mod p
     *  Therefore, raise to (p-2) = 2^255-21 to get a multiplicative inverse.
     *
     *  This is a 255-bit binary number with the digits: 11111111... 01011
     *
     *  We compute the result by the usual binary chain, but alternate between keeping the accumulator in r and s,
     *  so as to avoid copying temporaries.
     */
    private static void invDistinct(short[] r, short[] x) {

        short[] s = new short[SIZE];

        /* 1 1 */
        mulDistinct(s, x, x);
        mulDistinct(r, s, x);

        /* 1 x 248 */
        for (int i = 0; i < 248; i++) {
            mulDistinct(s, r, r);
            mulDistinct(r, s, x);
        }

        /* 0 */
        mulDistinct(s, r, r);

        /* 1 */
        mulDistinct(r, s, s);
        mulDistinct(s, r, x);

        /* 0 */
        mulDistinct(r, s, s);

        /* 1 */
        mulDistinct(s, r, r);
        mulDistinct(r, s, x);

        /* 1 */
        mulDistinct(s, r, r);
        mulDistinct(r, s, x);
    }

    /**
     * Convert an Edwards Y to a Montgomery X (Edwards X is not used). Resulting coordinate is normalized.
     *
     * @return the montgomery X coordinates
     * */
    static byte[] ed25519ToCurve25519(byte[] edwardYBytes) {
        short[] edwardY = Convert.toUnsignedBytes(edwardYBytes);
        short[] yplus = new short[SIZE];
        short[] yminus = new short[SIZE];

        sub(yplus, edwardY);
        invDistinct(yminus, yplus);
        add(yplus, edwardY);
        short[] montgomeryX = new short[32];
        mulDistinct(montgomeryX, yplus, yminus);
        normalize(montgomeryX);
        return Convert.toSignedBytes(montgomeryX);
    }
}