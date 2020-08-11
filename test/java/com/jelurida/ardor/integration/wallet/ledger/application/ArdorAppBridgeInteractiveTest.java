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

import nxt.account.Account;
import nxt.addons.JO;
import nxt.http.callers.BroadcastTransactionCall;
import nxt.http.callers.GetBalanceCall;
import nxt.http.callers.ParseTransactionCall;
import nxt.http.callers.SendMoneyCall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * To run these tests, you must have a ledger device connected and an Ardor app installed.
 * The ledger device should be unlocked and the Ardor app should be open on the device before starting the test.
 * The tests will display interactive data on the ledger device and ask for confirmations.
 */
public class ArdorAppBridgeInteractiveTest extends AbstractArdorAppBridgeTest {

    @BeforeClass
    public static void initDevice() {
        app = ArdorAppBridge.getApp();
    }

    @Before
    public void checkDeviceStatus() {
        Assert.assertTrue(app.getLastError(), app instanceof ArdorAppBridge);
    }

    @Test
    public void signTransaction() {
        // Get a new account public key from the ledger and fund it
        byte[] ledgerPublicKey = app.getWalletPublicKeys(PATH_STR_0, false);
        long ledgerAccountId = Account.getId(ledgerPublicKey);
        JO sendMoneyResponse1 = SendMoneyCall.create(2).recipient(ledgerAccountId).amountNQT(400000000).feeNQT(100000000).secretPhrase(ALICE.getSecretPhrase()).call();
        Logger.logInfoMessage(sendMoneyResponse1.toJSONString());
        generateBlock();

        // Create transaction bytes and sign them on the ledger
        JO sendMoneyResponse2 = SendMoneyCall.create(2).recipient(BOB.getStrId()).amountNQT(200000000).feeNQT(100000000).publicKey(ledgerPublicKey).call();
        String unsignedBytesHex = sendMoneyResponse2.getString("unsignedTransactionBytes");
        Logger.logInfoMessage("Confirm transaction on ledger device");
        boolean isLoaded = app.loadWalletTransaction(unsignedBytesHex);
        if (!isLoaded) {
            Logger.logInfoMessage("If the transaction signing was cancelled on the ledger by the user then all is well");
            return;
        }
        byte[] signature = app.signWalletTransaction(PATH_STR_0);
        String signatureHex = Convert.toHexString(signature);

        // Insert the signature into the transaction bytes and verify the signature
        int sigPos = 2 * SIGNATURE_POSITION;
        int sigLen = 2 * SIGNATURE_LENGTH;
        String signedBytesHex = unsignedBytesHex.substring(0, sigPos) + signatureHex + unsignedBytesHex.substring(sigPos + sigLen);
        JO response = ParseTransactionCall.create().transactionBytes(signedBytesHex).call();
        Assert.assertTrue(response.getBoolean("verify"));

        // Broadcast the transaction and check that it updated the ledger account balance
        JO broadcastTransactionResponse = BroadcastTransactionCall.create().transactionBytes(signedBytesHex).call();
        String fullHash = broadcastTransactionResponse.getString("fullHash");
        Assert.assertNotNull(fullHash);
        generateBlock();
        JO getBalanceResponse = GetBalanceCall.create(2).account(ledgerAccountId).call();
        Assert.assertEquals("100000000", getBalanceResponse.getString("balanceNQT"));
    }

    @Test
    public void showAddress() {
        app.showAddress(PATH_STR_0);
        Assert.assertTrue(true);
    }

}
