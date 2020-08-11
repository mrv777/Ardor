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

public final class DecryptedData {

    private final byte[] data;
    private final byte[] sharedKey;

    public DecryptedData(byte[] data, byte[] sharedKey) {
        this.data = data;
        this.sharedKey = sharedKey;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getSharedKey() {
        return sharedKey;
    }

    @Override
    public String toString() {
        return "data: " + Convert.toHexString(data) + " sharedKey: " + Convert.toHexString(sharedKey);
    }

}
