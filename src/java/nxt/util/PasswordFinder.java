/*
 * Copyright © 2020 Jelurida IP B.V.
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
import java.nio.charset.StandardCharsets;

public class PasswordFinder {

    public static int process(ByteBuffer buffer, String... secrets) {
        try {
            int[] pos = new int[secrets.length];
            byte[][] tokens = new byte[secrets.length][];
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = secrets[i].getBytes(StandardCharsets.UTF_8);
            }
            while (buffer.hasRemaining()) {
                byte current = buffer.get();
                for (int i = 0; i < tokens.length; i++) {
                    if (current != tokens[i][pos[i]]) {
                        pos[i] = 0;
                        continue;
                    }
                    pos[i]++;
                    if (pos[i] == tokens[i].length) {
                        return buffer.position() - tokens[i].length;
                    }
                }
            }
            return -1;
        } finally {
            buffer.rewind();
        }
    }
}
