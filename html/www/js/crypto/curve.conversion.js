/******************************************************************************
 * Copyright © 2016-2020 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of this software, including this file, may be copied, modified,    *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

let CurveConversion = function () {

    const SIZE = 32;
    const ONE = new Uint8Array(SIZE);
    ONE[0] = 1;

    function select(dst, zero, one, condition) {
        let mask = -condition;
        for (let i = 0; i < SIZE; i++) {
            dst[i] = zero[i] ^ (mask & (one[i] ^ zero[i]));
        }
    }

    function normalize(x) {
        /* Reduce using 2^255 = 19 mod p */
        let c = (x[31] >> 7) * 19;
        x[31] &= 0x7f;

        let i;
        for (i = 0; i < SIZE; i++) {
            c += x[i];
            x[i] = c & 0xff;
            c >>= 8;
        }

        /* The number is now less than 2^255 + 18, and therefore less than 2p.
         * Try subtracting p, and conditionally load the subtracted value if underflow did not occur. */
        c = 19;

        let minusp = new Uint8Array(SIZE);
        for (i = 0; i + 1 < SIZE; i++) {
            c += x[i];
            minusp[i] = c & 0xff;
            c >>= 8;
        }

        c += x[i] - 0x80;
        minusp[31] = c & 0xff;

        /* Load x-p if no underflow */
        select(x, minusp, x, (c >> 15) & 1);
    }

    function add(r, b) {
        /* Add */
        let c = 0;
        for (let i = 0; i < SIZE; i++) {
            c >>= 8;
            c += ONE[i] + b[i];
            r[i] = c & 0xff;
        }

        /* Reduce with 2^255 = 19 mod p */
        r[31] &= 127;
        c = (c >> 7) * 19;

        for (let i = 0; i < SIZE; i++) {
            c += r[i];
            r[i] = c & 0xff;
            c >>= 8;
        }
    }

    function sub(r, b) {
        /* Calculate a + 2p - b, to avoid underflow */
        let c = 218;
        for (let i = 0; i + 1 < SIZE; i++) {
            c += 65280 + ONE[i] - b[i];
            r[i] = c & 0xff;
            c >>= 8;
        }

        c += ONE[31] - b[31];
        r[31] = c & 0x7f;
        c = (c >> 7) * 19;

        for (let i = 0; i < SIZE; i++) {
            c += r[i];
            r[i] = c & 0xff;
            c >>= 8;
        }
    }

    function mulDistinct(r, a, b) {
        let c = 0;
        for (let i = 0; i < SIZE; i++) {
            let j;
            c >>= 8;
            for (j = 0; j <= i; j++) {
                c += (a[j] & 0xff) * (b[i - j] & 0xff);
            }

            for (; j < SIZE; j++) {
                c += (a[j] & 0xff) * (b[i + SIZE - j] & 0xff) * 38;
            }

            r[i] = c & 0xff;
        }

        r[31] &= 127;
        c = (c >> 7) * 19;

        for (let i = 0; i < SIZE; i++) {
            c += r[i];
            r[i] = c & 0xff;
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
    function invDistinct(r, x) {

        let s = new Uint8Array(SIZE);

        /* 1 1 */
        mulDistinct(s, x, x);
        mulDistinct(r, s, x);

        /* 1 x 248 */
        for (let i = 0; i < 248; i++) {
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
    function ed25519ToCurve25519(edwardYBytes) {
        let edwardY = edwardYBytes;
        let yplus = new Uint8Array(SIZE);
        let yminus = new Uint8Array(SIZE);

        sub(yplus, edwardY);
        invDistinct(yminus, yplus);
        add(yplus, edwardY);
        let montgomeryX = new Uint8Array(SIZE);
        mulDistinct(montgomeryX, yplus, yminus);
        normalize(montgomeryX);
        return montgomeryX;
    }

    return {
        ed25519ToCurve25519: ed25519ToCurve25519
    };
}();

if (isNode) {
    module.exports = CurveConversion;
}