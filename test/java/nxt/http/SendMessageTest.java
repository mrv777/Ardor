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

package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.RequireNonePermissionPolicyTestsCategory;
import nxt.account.Account;
import nxt.blockchain.ChildChain;
import nxt.blockchain.Fee;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.http.callers.ReadMessageCall;
import nxt.http.callers.SendMessageCall;
import nxt.messaging.MessagingTransactionType.MessageEvent;
import nxt.util.Convert;
import nxt.util.JSONAssert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

import static nxt.blockchain.ChildChain.IGNIS;

public class SendMessageTest extends BlockchainTest {
    @Rule
    public final MessageListenerRule messageListenerRule = new MessageListenerRule();

    public static final String NON_EXISTENT_ACCOUNT_SECRET = "NonExistentAccount.jkgdkjgdjkfgfkjgfjkdfgkjjdk";
    public static final byte[] NON_EXISTENT_ACCOUNT_PRIVATE_KEY = Crypto.getPrivateKey(NON_EXISTENT_ACCOUNT_SECRET);
    public static final String ANOTHER_ACCOUNT_SECRET = "another" + NON_EXISTENT_ACCOUNT_SECRET;
    public static final byte[] ANOTHER_ACCOUNT_PRIVATE_KEY = Crypto.getPrivateKey(ANOTHER_ACCOUNT_SECRET);

    @Test
    public void sendMessage() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("recipient", BOB.getStrId()).
                param("message", "hello world").
                param("feeNQT", IGNIS.ONE_COIN).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("fullHash");
        JSONObject attachment = (JSONObject) ((JSONObject) response.get("transactionJSON")).get("attachment");
        Assert.assertEquals("hello world", attachment.get("message"));
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", BOB.getSecretPhrase()).
                param("transactionFullHash", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("message"));
    }

    @Test
    public void sendEncryptedMessage() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("recipient", BOB.getStrId()).
                param("messageToEncrypt", "hello world").
                param("feeNQT", IGNIS.ONE_COIN).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("fullHash");
        JSONObject attachment = (JSONObject) ((JSONObject) response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptedMessage");
        Assert.assertNotEquals(64, ((String) encryptedMessage.get("data")).length());
        Assert.assertNotEquals(32, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", BOB.getSecretPhrase()).
                param("transactionFullHash", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessage"));
    }

    @Test
    public void sendClientEncryptedMessage() {
        EncryptedData encryptedData = BOB.getAccount().encryptTo(ALICE.getPrivateKey(), Convert.toBytes("hello world"), true);
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("recipient", BOB.getStrId()).
                param("encryptedMessageData", Convert.toHexString(encryptedData.getData())).
                param("encryptedMessageNonce", Convert.toHexString(encryptedData.getNonce())).
                param("feeNQT", IGNIS.ONE_COIN).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("fullHash");
        JSONObject attachment = (JSONObject) ((JSONObject) response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptedMessage");
        Assert.assertNotEquals(64, ((String) encryptedMessage.get("data")).length());
        Assert.assertNotEquals(32, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", BOB.getSecretPhrase()).
                param("transactionFullHash", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessage"));
    }

    @Test
    public void sendEncryptedMessageToSelf() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("recipient", BOB.getStrId()).
                param("messageToEncryptToSelf", "hello world").
                param("feeNQT", IGNIS.ONE_COIN).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("fullHash");
        JSONObject attachment = (JSONObject) ((JSONObject) response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptToSelfMessage");
        Assert.assertNotEquals(64, ((String) encryptedMessage.get("data")).length());
        Assert.assertNotEquals(32, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("transactionFullHash", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessageToSelf"));
    }

    @Test
    public void sendClientEncryptedMessageToSelf() {
        EncryptedData encryptedData = ALICE.getAccount().encryptTo(ALICE.getPrivateKey(), Convert.toBytes("hello world"),  true);
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("recipient", BOB.getStrId()).
                param("encryptToSelfMessageData", Convert.toHexString(encryptedData.getData())).
                param("encryptToSelfMessageNonce", Convert.toHexString(encryptedData.getNonce())).
                param("feeNQT", IGNIS.ONE_COIN).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("fullHash");
        JSONObject attachment = (JSONObject) ((JSONObject) response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptToSelfMessage");
        Assert.assertEquals(64 + 32 /* data + hash */, ((String) encryptedMessage.get("data")).length());
        Assert.assertEquals(64, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("transactionFullHash", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessageToSelf"));
    }

    @Category(RequireNonePermissionPolicyTestsCategory.class)
    @Test
    public void publicKeyAnnouncement() {
        byte[] publicKey = Crypto.getPublicKey(Crypto.getPrivateKey(NON_EXISTENT_ACCOUNT_SECRET));
        String publicKeyStr = Convert.toHexString(publicKey);
        long id = Account.getId(publicKey);
        String rsAccount = Convert.rsAccount(id);

        JSONObject response = new APICall.Builder("getAccount").
                param("account", rsAccount).
                build().invoke();
        Logger.logDebugMessage("getAccount: " + response);
        Assert.assertEquals((long) 5, response.get("errorCode"));

        response = new APICall.Builder("sendMessage").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("recipient", rsAccount).
                param("recipientPublicKey", publicKeyStr).
                param("feeNQT", IGNIS.ONE_COIN).
                build().invokeNoError();
        Logger.logDebugMessage("sendMessage: " + response);
        generateBlock();

        response = new APICall.Builder("getAccount").
                param("account", rsAccount).
                build().invokeNoError();
        Logger.logDebugMessage("getAccount: " + response);
        Assert.assertEquals(publicKeyStr, response.get("publicKey"));
    }

    @Category(RequireNonePermissionPolicyTestsCategory.class)
    @Test
    public void sendFromNotExistingToExistingAccount() {
        Assert.assertFalse(Account.hasAccount(Account.getId(Crypto.getPublicKey(NON_EXISTENT_ACCOUNT_PRIVATE_KEY)), Nxt.getBlockchain().getHeight()));
        APICall.Builder builder = new APICall.Builder("sendMessage").
                param("secretPhrase", NON_EXISTENT_ACCOUNT_SECRET).
                param("message", "hello world").
                param("recipient", ALICE.getRsAccount()).
                feeNQT(ChildChain.IGNIS.ONE_COIN);
        JSONAssert result = new JSONAssert(builder.build().invoke());
        Assert.assertEquals("Not enough funds", result.str("errorDescription"));

        builder.feeNQT(0);
        result = new JSONAssert(builder.build().invoke());
        Logger.logDebugMessage("response" + result.getJson());
        Assert.assertEquals(Fee.NEW_ACCOUNT_FEE + Constants.ONE_FXT / 100, Convert.parseLong(result.getJson().get("minimumFeeFQT")));
        bundleTransactions(Collections.singletonList(result.fullHash()));

        generateBlock();
    }

    @Category(RequireNonePermissionPolicyTestsCategory.class)
    @Test
    public void sendFromNotExistingAccountToSelf() {
        Assert.assertFalse(Account.hasAccount(Account.getId(Crypto.getPublicKey(NON_EXISTENT_ACCOUNT_PRIVATE_KEY)), Nxt.getBlockchain().getHeight()));
        APICall.Builder builder = new APICall.Builder("sendMessage").
                param("secretPhrase", NON_EXISTENT_ACCOUNT_SECRET).
                param("message", "hello world").
                param("recipient", Account.getId(Crypto.getPublicKey(NON_EXISTENT_ACCOUNT_PRIVATE_KEY))).
                feeNQT(ChildChain.IGNIS.ONE_COIN);
        JSONAssert result = new JSONAssert(builder.build().invoke());
        Assert.assertEquals("Not enough funds", result.str("errorDescription"));

        builder.feeNQT(0);
        result = new JSONAssert(builder.build().invoke());
        Logger.logDebugMessage("response" + result.getJson());
        Assert.assertEquals(Fee.NEW_ACCOUNT_FEE + Constants.ONE_FXT / 100, Convert.parseLong(result.getJson().get("minimumFeeFQT")));
        bundleTransactions(Collections.singletonList(result.fullHash()));

        generateBlock();
    }

    @Category(RequireNonePermissionPolicyTestsCategory.class)
    @Test
    public void sendFromNotExistingToNotExistingAccount() {
        Assert.assertFalse(Account.hasAccount(Account.getId(Crypto.getPublicKey(NON_EXISTENT_ACCOUNT_PRIVATE_KEY)), Nxt.getBlockchain().getHeight()));
        Assert.assertFalse(Account.hasAccount(Account.getId(Crypto.getPublicKey(ANOTHER_ACCOUNT_PRIVATE_KEY)), Nxt.getBlockchain().getHeight()));
        APICall.Builder builder = new APICall.Builder("sendMessage").
                param("secretPhrase", NON_EXISTENT_ACCOUNT_SECRET).
                param("message", "hello world").
                param("recipient", Account.getId(Crypto.getPublicKey(ANOTHER_ACCOUNT_PRIVATE_KEY))).
                feeNQT(ChildChain.IGNIS.ONE_COIN);
        JSONAssert result = new JSONAssert(builder.build().invoke());
        Assert.assertEquals("Not enough funds", result.str("errorDescription"));

        builder.feeNQT(0);
        result = new JSONAssert(builder.build().invoke());
        Logger.logDebugMessage("response" + result.getJson());
        Assert.assertEquals(2 * Fee.NEW_ACCOUNT_FEE + Constants.ONE_FXT / 100, Convert.parseLong(result.getJson().get("minimumFeeFQT")));
        bundleTransactions(Collections.singletonList(result.fullHash()));

        generateBlock();
    }

    @Test
    public void listenToMessages() {
        ChildChain chain = IGNIS;
        SendMessageCall.create(chain.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .recipient(BOB.getStrId())
                .message("hello world")
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();
        generateBlock();

        MessageEvent actual = messageListenerRule.getEvents().get(0);
        Assert.assertEquals(ALICE.getId(), actual.getSenderAccount().getId());
        Assert.assertEquals(BOB.getAccount().getId(), actual.getRecipientAccount().getId());
        Assert.assertEquals(chain, actual.getChain());
        Assert.assertEquals("hello world", Convert.toString(actual.getMessage().getMessage()));
        Assert.assertEquals(1, messageListenerRule.getEvents().size());
    }

    @Test
    public void listenToMessagesNullMessage() {
        ChildChain chain = IGNIS;
        SendMessageCall.create(chain.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .recipient(BOB.getStrId())
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();
        generateBlock();

        MessageEvent actual = messageListenerRule.getEvents().get(0);
        Assert.assertEquals(ALICE.getId(), actual.getSenderAccount().getId());
        Assert.assertEquals(BOB.getAccount().getId(), actual.getRecipientAccount().getId());
        Assert.assertEquals(chain, actual.getChain());
        Assert.assertNull(actual.getMessage());
        Assert.assertEquals(1, messageListenerRule.getEvents().size());
	}

	@Test
	public void testForcedIsTextOfFileMessage() {
        SendMessageCall sendMessageCall = createSendMessageCall()
                .messageFile(Convert.toBytes("test message oymkbhnv"));

        Assert.assertEquals("Expected auto-detected text", true,
                getMessageIsText(sendMessageCall.build().invokeNoError(), false));

        sendMessageCall.messageIsText(false);
        Assert.assertEquals(false, getMessageIsText(sendMessageCall.build().invokeNoError(), false));

        sendMessageCall = createSendMessageCall()
                .messageFile(new byte[]{0, 0, 0, 1, 0, 4, 0, 127, 0, -127});

        Assert.assertEquals("Expected auto-detected non-text", false,
                getMessageIsText(sendMessageCall.build().invokeNoError(), false));

        sendMessageCall.messageIsText(true);
        Assert.assertEquals("Incorrect \"messageFile\" does not contain UTF-8 text",
                sendMessageCall.build().invokeWithError().getErrorDescription());
    }

    @Test
    public void testForcedIsTextOfEncryptedFileMessage() {
        SendMessageCall sendMessageCall = createSendMessageCall()
                .messageToEncryptFile(Convert.toBytes("test message rewio834"));

        Assert.assertEquals("Expected auto-detected text", true,
                getMessageIsText(sendMessageCall.build().invokeNoError(), true));

        sendMessageCall.messageToEncryptIsText(false);
        Assert.assertEquals(false, getMessageIsText(sendMessageCall.build().invokeNoError(), true));

        sendMessageCall = createSendMessageCall()
                .messageToEncryptFile(new byte[]{0, 0, 0, 1, 0, 4, 0, 127, 0, -127});

        Assert.assertEquals("Expected auto-detected non-text", false,
                getMessageIsText(sendMessageCall.build().invokeNoError(), true));

        sendMessageCall.messageToEncryptIsText(true);
        Assert.assertEquals("Incorrect \"messageToEncryptFile\" does not contain UTF-8 text",
                sendMessageCall.build().invokeWithError().getErrorDescription());

    }

    @Test
    public void testTwoFiles() {
        String message = "test message rewio834";
        String messageToSelf = "test message to self sdfnkoe";

        EncryptedData encryptedData = Account.encryptTo(ALICE.getPrivateKey(), ALICE.getPublicKey(), Convert.toBytes(messageToSelf), false);

        String fullHash = new JSONAssert(createSendMessageCall()
                .messageToEncryptFile(Convert.toBytes(message))
                .encryptToSelfMessageNonce(encryptedData.getNonce())
                .encryptToSelfMessageFile(encryptedData.getData())
                .compressMessageToEncryptToSelf("false")
                .build().invokeNoError()).fullHash();

        generateBlock();

        JSONAssert result = new JSONAssert(ReadMessageCall.create(IGNIS.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .transactionFullHash(fullHash).build().invokeNoError());

        Assert.assertEquals(message, result.str("decryptedMessage"));
        Assert.assertEquals(messageToSelf, result.str("decryptedMessageToSelf"));
    }

    private SendMessageCall createSendMessageCall() {
        return SendMessageCall.create(IGNIS.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .recipient(BOB.getStrId())
                .feeNQT(IGNIS.ONE_COIN);
    }

    private Boolean getMessageIsText(JSONObject result, boolean isEncrypted) {
        JSONAssert jsonAssert = new JSONAssert(result)
                .subObj("transactionJSON")
                .subObj("attachment");
        if (isEncrypted) {
            return jsonAssert.subObj("encryptedMessage").bool("isText");
        } else {
            return jsonAssert.bool("messageIsText");
        }
    }
}
