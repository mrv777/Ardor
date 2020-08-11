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
import nxt.addons.JO;
import nxt.crypto.BIP39Test;
import nxt.http.callers.CombineSecretCall;
import nxt.http.callers.SendMoneyCall;
import nxt.http.callers.SplitSecretCall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

public class SecretSharingApiTest extends BlockchainTest {
    @Test
    public void useApis2of3() {
        // Generate the pieces
        JO splitResponse = SplitSecretCall.create().secret(ALICE.getSecretPhrase()).totalPieces(3).minimumPieces(2).call();
        List<String> pieces = splitResponse.getArray("pieces").values();
        combine2of3(ALICE.getSecretPhrase(), pieces, "secret");
    }

    @Test
    public void useApis2of3BIP39() {
        for(BIP39Test.TestVector testVector : BIP39Test.testVectors) {
            if (testVector.getEntropy().compareTo(BigInteger.ZERO) != 0) {
                String secretPhrase = String.join(" ", testVector.getMnemonic());
                JO splitResponse = SplitSecretCall.create().secret(secretPhrase).totalPieces(3).minimumPieces(2).call();

                List<String> pieces = splitResponse.getArray("pieces").values();
                combine2of3(secretPhrase, pieces, "secret");
            }
        }
    }

    @Test
    public void useApis2of3PrivateKey() {
        // Generate the pieces
        JO splitResponse = SplitSecretCall.create().privateKey(BOB.getPrivateKey()).totalPieces(3).minimumPieces(2).call();
        String hexPrivateKey = Convert.toHexString(BOB.getPrivateKey());
        List<String> pieces = splitResponse.getArray("pieces").values();
        combine2of3(hexPrivateKey, pieces, "privateKey");
    }

    private void combine2of3(String secretPhrase, List<String> pieces, String responseField) {
        // Select pieces and combine
        JO combineResponse = CombineSecretCall.create().pieces(pieces.get(0), pieces.get(1)).call();
        Assert.assertEquals(secretPhrase, combineResponse.getString(responseField));

        // Select other pieces and combine
        combineResponse = CombineSecretCall.create().pieces(pieces.get(0), pieces.get(2)).call();
        Assert.assertEquals(secretPhrase, combineResponse.getString(responseField));
        combineResponse = CombineSecretCall.create().pieces(pieces.get(1), pieces.get(2)).call();
        Assert.assertEquals(secretPhrase, combineResponse.getString(responseField));
    }

    @Test
    public void useApis3of5() {
        // Generate the pieces
        JO splitResponse = SplitSecretCall.create().secret(CHUCK.getSecretPhrase()).totalPieces(5).minimumPieces(3).call();

        // Select pieces and combine
        List<String> pieces = splitResponse.getArray("pieces").values();
        Logger.logInfoMessage(pieces.toString());
        JO combineResponse = CombineSecretCall.create().pieces(pieces.get(1), pieces.get(3), pieces.get(4)).call();
        Assert.assertEquals(CHUCK.getSecretPhrase(), combineResponse.getString("secret"));
    }

    @Test
    public void useApis3of5BIP39() {
        for(BIP39Test.TestVector testVector : BIP39Test.testVectors) {
            if (testVector.getEntropy().compareTo(BigInteger.ZERO) != 0) {
                String secretPhrase = String.join(" ", testVector.getMnemonic());

                // Generate the pieces
                JO splitResponse = SplitSecretCall.create().secret(secretPhrase).totalPieces(5).minimumPieces(3).call();

                // Select pieces and combine
                List<String> pieces = splitResponse.getArray("pieces").values();
                Logger.logInfoMessage(pieces.toString());
                JO combineResponse = CombineSecretCall.create().pieces(pieces.get(1), pieces.get(3), pieces.get(4)).call();
                Assert.assertEquals(secretPhrase, combineResponse.getString("secret"));
            }
        }
    }

    @Test
    public void useApis3of5PrivateKey() {
        // Generate the pieces
        JO splitResponse = SplitSecretCall.create().privateKey(CHUCK.getPrivateKey()).totalPieces(5).minimumPieces(3).call();

        // Select pieces and combine
        List<String> pieces = splitResponse.getArray("pieces").values();
        Logger.logInfoMessage(pieces.toString());
        JO combineResponse = CombineSecretCall.create().pieces(pieces.get(1), pieces.get(3), pieces.get(4)).call();
        Assert.assertEquals(Convert.toHexString(CHUCK.getPrivateKey()), combineResponse.getString("privateKey"));
    }

    @Test
    public void missingPieces() {
        // Generate the pieces
        JO splitResponse = SplitSecretCall.create().secret(ALICE.getSecretPhrase()).totalPieces(3).minimumPieces(2).call();

        // Select pieces and combine
        List<String> pieces = splitResponse.getArray("pieces").values();
        JO combineResponse = CombineSecretCall.create().pieces(pieces.get(0)).call();
        Assert.assertNotEquals(ALICE.getSecretPhrase(), combineResponse.getString("secret"));
    }

    @Test
    public void wrongPieces() {
        // Generate the pieces
        JO splitResponse = SplitSecretCall.create().secret(CHUCK.getSecretPhrase()).totalPieces(3).minimumPieces(2).call();

        // Select pieces and combine correctly
        List<String> pieces = splitResponse.getArray("pieces").values();
        JO combineResponse = CombineSecretCall.create().pieces(pieces.get(0), pieces.get(1)).call();
        Assert.assertEquals(CHUCK.getSecretPhrase(), combineResponse.getString("secret"));

        // Now corrupt one of the pieces and see that the reproduced secret is wrong
        combineResponse = CombineSecretCall.create().pieces(pieces.get(0) + "AB", pieces.get(1)).call();
        Assert.assertNotEquals(CHUCK.getSecretPhrase(), combineResponse.getString("secret"));
    }

    /**
     * Submit a transaction with one secret phrase piece and the account id.
     * The other piece is loaded from nxt.properties and combined with this piece.
     * Piece values can be generated using the useApis2of3 test
     */
    @Test
    public void submitSecretPhrasePiece() {
        JO sendMoneyResponse = SendMoneyCall.create(2).recipient(BlockchainTest.BOB.getRsAccount()).amountNQT(123456789).feeNQT(1000000).
                sharedPiece("1:9999:3:2:0:3:626cef7a2bfe67d3ecc2168c32e8a460db5c2c71adaed3b9").sharedPieceAccount(ALICE.getRsAccount()).call();
        Assert.assertEquals("true", sendMoneyResponse.getString("broadcasted"));
    }

    /**
     * Submit a transaction with one secret piece for a private key and the account id.
     * The other piece is loaded from nxt.properties and combined with this piece.
     * Piece values can be generated using the useApis2of3 test
     */
    @Test
    public void submitPrivateKeyPiece() {
        JO sendMoneyResponse = SendMoneyCall.create(2).recipient(BlockchainTest.ALICE.getRsAccount()).amountNQT(123456789).feeNQT(1000000).
                sharedPiece("3:1539292261:3:2:0:2:00a1d4415680c2d34ee9bf1644f1f474138667995199828c15ebd6281108060a21").sharedPieceAccount(BOB.getRsAccount()).call();
        Assert.assertEquals("true", sendMoneyResponse.getString("broadcasted"));
    }

    /**
     * Submit a transaction with multiple secret phrase pieces.
     * Nothing is loaded from nxt.properties.
     * Piece values can be generated by the useApis3of5 test
     */
    @Test
    public void submitMultipleSecretPhrasePiece() {
        JO sendMoneyResponse = SendMoneyCall.create(2).recipient(BlockchainTest.BOB.getRsAccount()).amountNQT(123456789).feeNQT(1000000).
                sharedPiece("0:-1797511508:5:3:0:1:00994e4109d68fba8ed92c1b65b7c50963d4480f623de5d5d1f230eb199ac37c088f3bc3",
                        "0:-1797511508:5:3:0:2:01135524eff764421052eb34520b6f009588ad5bb3f707443dacccab51b430283513ee4a",
                        "0:-1797511508:5:3:0:5:042599fb5a1d4876b15b8e1705864273d5395e7f05f3e2bd163c5a2b4cf34adade2c0e33").
                sharedPieceAccount(CHUCK.getRsAccount()).call();
        Assert.assertEquals("true", sendMoneyResponse.getString("broadcasted"));
    }

    /**
     * Submit a transaction with multiple secret phrase pieces for a private key.
     * Nothing is loaded from nxt.properties.
     * Piece values can be generated by the useApis3of5 test
     */
    @Test
    public void submitMultipleSecretPrivateKey() {
        JO sendMoneyResponse = SendMoneyCall.create(2).recipient(BlockchainTest.BOB.getRsAccount()).amountNQT(123456789).feeNQT(1000000).
                sharedPiece("3:1904865159:5:3:0:2:00d5b5c5340ddd65f4de8fb69ddcd316963d994e6156bbf55f695f5afe22dfa4",
                        "3:1904865159:5:3:0:3:01080e4754f642942f0f5213543454d1e26a2f1280b9c35499d372e5d1216da8",
                        "3:1904865159:5:3:0:4:0149c465f7e9d5fa03dce350bc1fce696512f62c45595596683b7c986afe16ea").
                sharedPieceAccount(CHUCK.getRsAccount()).call();
        Assert.assertEquals("true", sendMoneyResponse.getString("broadcasted"));
    }
}
