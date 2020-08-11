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

public interface ArdorAppInterface {
    byte[] getWalletPublicKeys(String path, boolean isGetMasterKey);
    PublicKeyData getPublicKeyData(String pathStr);
    PublicKeyData getPublicKeyData(int[] path, boolean isGetPublicKeyAndChainCode);
    boolean loadWalletTransaction(String buffer);
    byte[] signWalletTransaction(String path);
    String encryptBuffer(String path, String publicKeyHex, String messageBytesHex);
    String decryptBuffer(String path, String publicKeyHex, String nonceHex, String dataHex);
    void showAddress(String pathStr);
    String signToken(String pathStr, int timestamp, String messageHex);

    String getLastError();
}
