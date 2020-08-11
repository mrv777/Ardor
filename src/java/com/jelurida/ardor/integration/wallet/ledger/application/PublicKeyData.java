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

package com.jelurida.ardor.integration.wallet.ledger.application;

import nxt.crypto.SerializedMasterPublicKey;

import static nxt.util.Convert.EMPTY_BYTE;

public class PublicKeyData {
    private final byte[] curve25519PublicKey;
    private final byte[] ed25519PublicKey;
    private final byte[] chainCode;
    private final byte[] serializedMasterPublicKey;

    PublicKeyData(byte[] result) {
        if (result == null || result.length == 0) {
            curve25519PublicKey = EMPTY_BYTE;
            ed25519PublicKey = EMPTY_BYTE;
            chainCode = EMPTY_BYTE;
            serializedMasterPublicKey = EMPTY_BYTE;
            return;
        }
        curve25519PublicKey = new byte[32];
        System.arraycopy(result, 1, curve25519PublicKey, 0, curve25519PublicKey.length);
        if (result.length == 1 + 3*32) {
            ed25519PublicKey = new byte[32];
            System.arraycopy(result, 1 + 32, ed25519PublicKey, 0, ed25519PublicKey.length);
            chainCode = new byte[32];
            System.arraycopy(result, 1 + 32 + 32, chainCode, 0, chainCode.length);
            serializedMasterPublicKey = new SerializedMasterPublicKey(ed25519PublicKey, chainCode).getSerializedMasterPublicKey();
        } else {
            ed25519PublicKey = EMPTY_BYTE;
            chainCode = EMPTY_BYTE;
            serializedMasterPublicKey = EMPTY_BYTE;
        }
    }

    public byte[] getCurve25519PublicKey() {
        return curve25519PublicKey;
    }

    public byte[] getEd25519PublicKey() {
        return ed25519PublicKey;
    }

    public byte[] getChainCode() {
        return chainCode;
    }

    public byte[] getSerializedMasterPublicKey() {
        return serializedMasterPublicKey;
    }

    public byte[] getData() {
        byte[] data = new byte[3*32];
        System.arraycopy(curve25519PublicKey, 0, data, 0, 32);
        System.arraycopy(ed25519PublicKey, 0, data, 32, 32);
        System.arraycopy(chainCode, 0, data, 64, 32);
        return data;
    }
}
