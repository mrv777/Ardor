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

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.account.Token;
import nxt.crypto.DecryptedData;
import nxt.crypto.EncryptedData;
import nxt.crypto.KeyDerivation;
import nxt.util.Bip32Path;
import nxt.util.Convert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

/**
 * To run these tests, you must have a ledger device connected and an Ardor app installed.
 * The ledger device should be unlocked and the Ardor app should be open on the device before starting the test.
 * The tests should not display any data on the ledger device therefore can run without operator assistance.
 */
public class ArdorAppBridgeHeadlessTest extends AbstractArdorAppBridgeTest {

    private static String EXPECTED_PUBLIC_KEY_0;
    private static String EXPECTED_PUBLIC_KEY_3;

    @BeforeClass
    public static void initDevice() {
        app = ArdorAppBridge.getApp();
        Bip32Path rootPath = Constants.ARDOR_TESTNET_BIP32_ROOT_PATH;
        KeyDerivation.Bip32Node parentNode = KeyDerivation.deriveMnemonic(rootPath.toString(), MNEMONIC);
        EXPECTED_PUBLIC_KEY_0 = Convert.toHexString(KeyDerivation.deriveChildPublicKey(parentNode, 0).getPublicKey());
        EXPECTED_PUBLIC_KEY_3 = Convert.toHexString(KeyDerivation.deriveChildPublicKey(parentNode, 3).getPublicKey());
    }

    @Before
    public void checkDeviceStatus() {
        Assert.assertTrue(app.getLastError(), app instanceof ArdorAppBridge);
    }

    @Test
    public void getVersion() {
        Assert.assertEquals("", app.getLastError());
        Assert.assertTrue(app instanceof ArdorAppBridge);
        Assert.assertFalse(((ArdorAppBridge)app).isInvalidAppSignature());
    }

    @Test
    public void getPublicKeys() {
        // Generate single key
        Assert.assertEquals(EXPECTED_PUBLIC_KEY_0, Convert.toHexString(app.getWalletPublicKeys(PATH_STR_0, false)));
        Assert.assertEquals(EXPECTED_PUBLIC_KEY_3, Convert.toHexString(app.getWalletPublicKeys(PATH_STR_3, false)));
    }

    @Test
    public void ledgerEncryptDecrypt() {
        String plainText = "LYLY";
        byte[] recipientPublicKey = app.getWalletPublicKeys(PATH_STR_3, false);
        Assert.assertEquals(recipientPublicKey.length, 32);
        String result = app.encryptBuffer(PATH_STR_0, Convert.toHexString(recipientPublicKey), Convert.toHexString(plainText.getBytes()));
        Assert.assertNotNull(result);
        String[] tokens = result.split(",");
        String dataHex = tokens[0];
        String nonceHex = tokens[1];
        byte[] senderPublicKey = app.getWalletPublicKeys(PATH_STR_0, false);
        String decryptedData = app.decryptBuffer(PATH_STR_3, Convert.toHexString(senderPublicKey), nonceHex, dataHex);
        tokens = decryptedData.split(",");
        Assert.assertArrayEquals(Convert.parseHexString(tokens[0]), plainText.getBytes());
    }

    @Test
    public void ledgerEncryptDecryptLongMessage() {
        // Now let's do it again with a longer message
        byte[] dataToEncrypt = new byte[1024];
        Random r = new Random();
        r.nextBytes(dataToEncrypt);
        byte[] recipientPublicKey = app.getWalletPublicKeys(PATH_STR_3, false);
        Assert.assertEquals(recipientPublicKey.length, 32);
        String result = app.encryptBuffer(PATH_STR_0, Convert.toHexString(recipientPublicKey), Convert.toHexString(dataToEncrypt));
        Assert.assertNotNull(result);
        String[] tokens = result.split(",");
        String dataHex = tokens[0];
        String nonceHex = tokens[1];
        byte[] senderPublicKey = app.getWalletPublicKeys(PATH_STR_0, false);
        String decryptedData = app.decryptBuffer(PATH_STR_3, Convert.toHexString(senderPublicKey), nonceHex, dataHex);
        tokens = decryptedData.split(",");
        Assert.assertArrayEquals(Convert.parseHexString(tokens[0]), dataToEncrypt);
    }

    @Test
    public void ledgerEncryptJavaDecrypt() {
        byte[] dataToEncrypt = new byte[64];
        Random r = new Random();
        r.nextBytes(dataToEncrypt);
        byte[] recipientPublicKey = BlockchainTest.ALICE.getPublicKey();
        Assert.assertEquals(recipientPublicKey.length, 32);
        EncryptedData encryptedData = ((ArdorAppBridge)app).encryptBuffer(Bip32Path.bip32StrToPath(PATH_STR_0), recipientPublicKey, dataToEncrypt);
        byte[] senderPublicKey = app.getWalletPublicKeys(PATH_STR_0, false);
        byte[] decryptedMessage = encryptedData.decrypt(BlockchainTest.ALICE.getPrivateKey(), senderPublicKey);
        decryptedMessage = Convert.uncompress(decryptedMessage);
        Assert.assertArrayEquals(decryptedMessage, dataToEncrypt);
    }

    @Test
    public void javaEncryptLedgerDecrypt() {
        byte[] recipientPublicKey = app.getWalletPublicKeys(PATH_STR_3, false);
        EncryptedData encryptedData = EncryptedData.encrypt(Convert.compress(DATA_TO_ENCRYPT), BlockchainTest.ALICE.getPrivateKey(), recipientPublicKey);
        DecryptedData decryptedData = ((ArdorAppBridge)app).decryptBuffer(Bip32Path.bip32StrToPath(PATH_STR_3), BlockchainTest.ALICE.getPublicKey(), encryptedData);
        Assert.assertArrayEquals(decryptedData.getData(), DATA_TO_ENCRYPT);
    }

    @Test
    public void generateToken() {
        int timestamp = Convert.toEpochTime(System.currentTimeMillis());
        String tokenData = "Token Data";
        String tokenStr = app.signToken(PATH_STR_0, timestamp, Convert.toHexString(Convert.toBytes(tokenData)));
        Token token = Token.parseToken(tokenStr, tokenData);
        Assert.assertTrue(token.isValid());
        Assert.assertEquals(token.getTimestamp(), timestamp);
        Assert.assertArrayEquals(token.getPublicKey(), app.getWalletPublicKeys(PATH_STR_0, false));
    }

    @Test
    public void generateTokenFromLargeDataSet() {
        Random r = new Random(1);
        int timestamp = r.nextInt();
        byte[] blob = new byte[40000];
        r.nextBytes(blob);
        String tokenStr = app.signToken(PATH_STR_3, timestamp, Convert.toHexString(blob));
        Token token = Token.parseToken(tokenStr, blob);
        Assert.assertTrue(token.isValid());
        Assert.assertEquals(token.getTimestamp(), timestamp);
        Assert.assertArrayEquals(token.getPublicKey(), app.getWalletPublicKeys(PATH_STR_3, false));
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    public void parsePath() {
        Assert.assertArrayEquals(new int[]{ 0x2C + Constants.HARDENED, 0x4172 + Constants.HARDENED, 0x00 + Constants.HARDENED, 0x01 + Constants.HARDENED, 0x00 }, Bip32Path.bip32StrToPath(PATH_STR_0));
        Assert.assertEquals(PATH_STR_0, Bip32Path.fromString("m/44'/16754'/0'/1'/0").toString());
    }

    @Test
    public void keyDerivation() {
        deriveChildKeys(PATH_STR_PARENT_1, 4);
        deriveChildKeys(PATH_STR_PARENT_2, 3);
    }

    private void deriveChildKeys(String parentPath, int numKeys) {
        // Get master key for parent path
        PublicKeyData parentPublicKeyData = app.getPublicKeyData(parentPath);
        if (parentPublicKeyData.getEd25519PublicKey().length == 0) {
            Assert.fail("Perhaps ledger is disconnected or locked or not in ardor app?");
        }
        for (int i=0; i < numKeys; i++) {
            deriveChildKey(parentPublicKeyData.getEd25519PublicKey(), parentPublicKeyData.getChainCode(), i, parentPath);
        }
    }

    private void deriveChildKey(byte[] masterPublicKey, byte[] chainCode, int childIndex, String parentPath) {
        // Derive the ed25519 child public key using Java code
        KeyDerivation.Bip32Node bip32NodeData = KeyDerivation.deriveChildPublicKey(masterPublicKey, chainCode, childIndex);

        // We need the curve25519 public key
        byte[] curve25519PublicKeyBytes = bip32NodeData.getPublicKey();

        // Make sure the curve25519 key calculated in Java is identical to the key returned by ledger from the same path
        // when asked for directly. This proves that the keys generated offline are identical to the keys generated by ledger.
        byte[] childCurve25519PublicKey = app.getWalletPublicKeys(parentPath + "/" + childIndex, false);
        Assert.assertEquals(Convert.toHexString(childCurve25519PublicKey), Convert.toHexString(curve25519PublicKeyBytes));
    }

}
