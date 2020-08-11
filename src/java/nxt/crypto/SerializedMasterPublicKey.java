/*
 * Copyright © 2013-2016 The Nxt Core Developers.
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

import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Combination of a BIP32 Master Public Key + Chain Code along with a CRC32 checksum.
 */
public class SerializedMasterPublicKey {
    private final byte[] serializedMasterPublicKey;

    public SerializedMasterPublicKey(byte[] serializedMasterPublicKey) {
        if (!isValidSerializedMasterPublicKey(serializedMasterPublicKey)) {
            throw new IllegalArgumentException("Invalid serializedMasterPublicKey.");
        }
        this.serializedMasterPublicKey = serializedMasterPublicKey;
    }

    public SerializedMasterPublicKey(byte[] masterPublicKey, byte[] chainCode) {
        if (masterPublicKey == null || masterPublicKey.length != 32) {
            throw new IllegalArgumentException("Invalid masterPublicKey.");
        }
        if (chainCode == null || chainCode.length != 32) {
            throw new IllegalArgumentException("Invalid chainCode.");
        }
        serializedMasterPublicKey = new byte[68];
        System.arraycopy(masterPublicKey, 0, serializedMasterPublicKey, 0, 32);
        System.arraycopy(chainCode, 0, serializedMasterPublicKey, 32, 32);
        CRC32 crc32 = new CRC32();
        crc32.update(serializedMasterPublicKey, 0, 64);
        System.arraycopy(Convert.toBytes(crc32.getValue()), 0, serializedMasterPublicKey, 64, 4);
    }

    public static boolean isValidSerializedMasterPublicKey(byte[] serializedMasterPublicKey) {
        if (serializedMasterPublicKey == null || serializedMasterPublicKey.length != 68) {
            return false;
        }

        CRC32 crc32 = new CRC32();
        crc32.update(serializedMasterPublicKey, 0, 64);
        byte[] checksum = Convert.toBytes(crc32.getValue());
        for(int i = 0; i < 4; i++) {
            if (checksum[i] != serializedMasterPublicKey[i + 64]) {
                return false;
            }
        }
        return true;
    }

    public byte[] getSerializedMasterPublicKey() {
        return serializedMasterPublicKey;
    }

    public byte[] getMasterPublicKey() {
        return Arrays.copyOfRange(serializedMasterPublicKey, 0, 32);
    }

    public byte[] getChainCode() {
        return Arrays.copyOfRange(serializedMasterPublicKey, 32, 64);
    }
}
