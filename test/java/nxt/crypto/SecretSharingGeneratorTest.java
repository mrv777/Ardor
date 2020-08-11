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

import nxt.BlockchainTest;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecretSharingGeneratorTest {
    private static final BigInteger ALICE_SECRET_PHRASE_128_BIT = new BigInteger("298106192037605529109565170145082624171");
    private static final String aliceSecretPhrase = BlockchainTest.aliceSecretPhrase;
    private static final String chuckSecretPhrase = BlockchainTest.chuckSecretPhrase;

    @Test
    public void secretPhraseTwelveWordsToBigIntegerAndBack() {
        BigInteger bigInteger = SecretSharingGenerator.Version.LEGACY_WORDS.secretToNumber(aliceSecretPhrase);
        String secretPhrase = SecretSharingGenerator.Version.LEGACY_WORDS.numberToSecret(bigInteger);
        Assert.assertEquals(aliceSecretPhrase, secretPhrase);
        BigInteger n128 = SecretSharingGenerator.Version.LEGACY_WORDS.secretToNumber(secretPhrase);
        Assert.assertEquals(ALICE_SECRET_PHRASE_128_BIT, n128);
        secretPhrase = SecretSharingGenerator.Version.LEGACY_WORDS.numberToSecret(n128);
        Assert.assertEquals(aliceSecretPhrase, secretPhrase);
    }

    @Test
    public void splitAndCombine12wordsSecretPhrase() {
        splitAndCombineSecretPhrase(aliceSecretPhrase);
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitAndCombineLegacyAllZeroWordSecretPhrase() {
        splitAndCombineSecretPhrase(Stream.generate(() -> "like").limit(12).collect(Collectors.joining(" ")));
    }

    @Test
    public void splitAndCombineBIP39SecretPhrase() {
        for (BIP39Test.TestVector testVector : BIP39Test.testVectors) {
            if (testVector.getEntropy().compareTo(BigInteger.ZERO) != 0) {
                splitAndCombineSecretPhrase(String.join(" ",testVector.getMnemonic()));
            }
        }
    }

    private void splitAndCombineSecretPhrase(String secretPhrase) {
        // Generate the pieces
        String[] pieces = SecretSharingGenerator.split(secretPhrase, 5, 3, BigInteger.ZERO);

        // Select pieces and combine
        String[] selectedPieces = new String[]{pieces[0], pieces[2], pieces[4]};
        String combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(secretPhrase, combinedSecret);

        // Select pieces and combine
        selectedPieces = new String[]{pieces[0], pieces[2], pieces[4]};
        combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(secretPhrase, combinedSecret);

        // Again with 2 out of 3
        pieces = SecretSharingGenerator.split(secretPhrase, 3, 2, BigInteger.ZERO);
        selectedPieces = new String[]{pieces[0], pieces[2]};
        combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(secretPhrase, combinedSecret);
    }

    @Test
    public void splitAndCombineRandomSecretPhrase() {
        // Generate the pieces
        String[] pieces = SecretSharingGenerator.split(chuckSecretPhrase, 7, 4, BigInteger.ZERO);

        // Select pieces and combine
        String[] selectedPieces = new String[]{pieces[1], pieces[3], pieces[5], pieces[6]};
        String combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(chuckSecretPhrase, combinedSecret);

        // Select pieces and combine
        selectedPieces = new String[]{pieces[1], pieces[2], pieces[4], pieces[6]};
        combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(chuckSecretPhrase, combinedSecret);
    }

    @Test
    public void splitAndCombineRedundantPieces() {
        // Generate the pieces
        String[] pieces = SecretSharingGenerator.split(chuckSecretPhrase, 7, 4, BigInteger.ZERO);

        // Select pieces and combine
        String[] selectedPieces = new String[]{pieces[1], pieces[2], pieces[3], pieces[5], pieces[6]};
        String combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(chuckSecretPhrase, combinedSecret);
    }

    /**
     * This should work assuming we do not validate and throw exception when the min number of pieces to combine is too small
     * inside combine.
     * Comment out this test and comment the check for min number of pieces inside combine.
     */
//    @Test
//    public void splitAndCombineNotEnoughPieces() {
//        // Generate the pieces
//        String[] pieces = SecretSharingGenerator.split(chuckSecretPhrase, 7, 4, BigInteger.ZERO);
//
//        // Select pieces and combine
//        String[] selectedPieces = new String[]{pieces[1], pieces[5], pieces[6]};
//        String combinedSecret = SecretSharingGenerator.combine(selectedPieces);
//        Assert.assertNotEquals(chuckSecretPhrase, combinedSecret);
//    }

    @Test
    public void shortPassphrase() {
        String[] pieces = SecretSharingGenerator.split("aaa", 7, 4, BigInteger.ZERO);
        System.out.println(Arrays.toString(pieces));
        String[] selectedPieces = new String[]{pieces[1], pieces[2], pieces[4], pieces[6]};
        String combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals("aaa", combinedSecret);
    }

    @Test
    public void splitAndCombineCustomStringWithSpace() {
        String secret = "lorem ipsum";
        String[] pieces = SecretSharingGenerator.split(secret, 3, 2, BigInteger.ZERO);

        String[] selectedPieces = new String[]{pieces[0], pieces[1]};
        String combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(secret, combinedSecret);

        selectedPieces = new String[]{pieces[2], pieces[1]};
        combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(secret, combinedSecret);

        selectedPieces = new String[]{pieces[0], pieces[2]};
        combinedSecret = SecretSharingGenerator.combine(selectedPieces);
        Assert.assertEquals(secret, combinedSecret);
    }

    @Test
    public void validityChecks() {
        try {
            SecretSharingGenerator.split(aliceSecretPhrase, 4, 1, BigInteger.ZERO);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            SecretSharingGenerator.split(aliceSecretPhrase, 4, 5, BigInteger.ZERO);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void splitAndCombinePrivateKeys() {
        splitAndCombineByteArray(Crypto.getPrivateKey(BlockchainTest.aliceSecretPhrase));
        splitAndCombineByteArray(Crypto.getPrivateKey(BlockchainTest.bobSecretPhrase2));
        splitAndCombineByteArray(Crypto.getPrivateKey(BlockchainTest.chuckSecretPhrase));
        splitAndCombineByteArray(Crypto.getPrivateKey(BlockchainTest.daveSecretPhrase));
        splitAndCombineByteArray(Crypto.getPrivateKey(BlockchainTest.rikerSecretPhrase));
    }

    private void splitAndCombineByteArray(byte[] secret) {
        String[] pieces = SecretSharingGenerator.split(secret, 3, 2, BigInteger.ZERO);

        String[] selectedPieces = new String[]{pieces[0], pieces[1]};
        byte[] combinedBytes = SecretSharingGenerator.combinePrivateKey(selectedPieces);
        Assert.assertArrayEquals(secret, combinedBytes);

        selectedPieces = new String[]{pieces[2], pieces[1]};
        combinedBytes = SecretSharingGenerator.combinePrivateKey(selectedPieces);
        Assert.assertArrayEquals(secret, combinedBytes);

        selectedPieces = new String[]{pieces[2], pieces[0]};
        combinedBytes = SecretSharingGenerator.combinePrivateKey(selectedPieces);
        Assert.assertArrayEquals(secret, combinedBytes);
    }

    @Test
    public void combineFromJavascriptShares() {
        String[] pieces = new String[]{
                "1:1330473566:3:2:0:1:16ec2bf03a3eb0bd4976b0e8a33c81ed1",
                "1:1330473566:3:2:0:2:1fd4053ff3c790cb239ea389d6d0df4f7",
                "1:1330473566:3:2:0:3:28bbde8fad5070d8fdc6962b0a653cb1d"
        };
        String combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("alice", aliceSecretPhrase, combinedPassphrase);

        pieces = new String[]{
                "0:141940365:3:2:0:1:94079b6178872e30498bcaa3",
                "0:141940365:3:2:0:2:b59bce4bb7acf9ec22a43414",
                "0:141940365:3:2:0:3:d7300135f6d2c5a7fbbc9d85"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("bob", BlockchainTest.bobSecretPhrase2, combinedPassphrase);

        pieces = new String[]{
                "0:511261255:3:2:0:1:c4d6c3329a4649c0ff56dc38d2c6df6939df74cc9d402dbc3c5c7e13025c3b765b4dd2",
                "0:511261255:3:2:0:2:1245e2222de40461a855a71fb2c246a593b469c23e82817240684c6b6b03e34b56a665a",
                "0:511261255:3:2:0:3:183e58113223a42740b5e07bd8581f5493cadc37b3310008bd0ad0f5a5e202df4797ee2"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("chuck", BlockchainTest.chuckSecretPhrase, combinedPassphrase);

        pieces = new String[]{
                "0:1686278125:3:2:0:1:db77cbdb4bd4a63436ffd68bc37c558722977d0ebca6ef635a5e1ff5b7545b314d5ba2",
                "0:1686278125:3:2:0:2:142b650841e3c08fb298c5bae1cc15499ebc58bac06eb9783657acba72a67830b3168d2",
                "0:1686278125:3:2:0:3:1a9f4d52cf0a36bc21c18e0d0760653acb4f39a4951303fa3709777589d7aaae5157602"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("dave", BlockchainTest.daveSecretPhrase, combinedPassphrase);

        pieces = new String[]{
                "0:2053746384:3:2:0:1:5a09c5a99111386793ba16c665023f9fd78788ff1022fd6e893e934508d07a32039bef4fc23cf9b97c485dc6a11de0eb0989",
                "0:2053746384:3:2:0:2:7eab21e9bae92e7ec31abe4a4fad10d67ebdc19cdd018c6ddc25ac59bb39bbf4aefe65522121a108b3284e21ccea766d9cd0",
                "0:2053746384:3:2:0:3:a34c7e29e4c12495f27b65ce3a57e20d25f3fa3aa9e01b6d2f0cc56e6da2fdb75a60db5480064857ea083e7cf8b70bf03017"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("riker", BlockchainTest.rikerSecretPhrase, combinedPassphrase);
    }

    @Test
    public void combineFromJavascriptSharesBIP39() {
        String[] pieces = new String[]{
                "2:936427405:3:2:0:1:d1f06f8bc795421618eb41c6f6f00379b6",
                "2:936427405:3:2:0:3:176d24fa457c0c7434bc2c655e5d10b6f0a"
        };
        String combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 0", "legal winner thank year wave sausage worth useful legal winner thank yellow", combinedPassphrase);
        pieces = new String[]{
                "2:2055486855:3:2:0:1:a5a111a20abff9908761fcf3cd07faa232",
                "2:2055486855:3:2:0:3:efe233e51f3eebb09524f5da6616eee67e"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 1", "letter advice cage absurd amount doctor acoustic avoid letter advice cage above", combinedPassphrase);
        pieces = new String[]{
                "2:255077422:3:2:0:1:15212088c8d2de890f1c8a8cb0ec1dabdaa",
                "2:255077422:3:2:0:3:1f63619a5a789b9b2d559fa612c45903ae6"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 2", "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong", combinedPassphrase);
        pieces = new String[]{
                "2:1758731957:3:2:0:1:fe3d266cf005989e9e00b79f072ba7a8f9b300b5eb328c5c4f",
                "2:1758731957:3:2:0:3:1fbb87447d111cadcdb0327de1683f7fbee1a0322c298a616c9"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 3", "legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth useful legal will", combinedPassphrase);
        pieces = new String[]{
                "2:9703170:3:2:0:1:ba5e322caba367704e00349cd1688f50ebac8ef3a6684ad236",
                "2:9703170:3:2:0:3:12e19958501e9354fe8ff9cd57338acf1c204abd9f237df767e"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 4", "letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter always", combinedPassphrase);
        pieces = new String[]{
                "2:252473937:3:2:0:1:12134a263f4459540a61d5c2fcff6cf16c8a2318c537678d3b8",
                "2:252473937:3:2:0:3:1639de72bdcd0bfc1f258148f6fe46d4459e694a4fa636a7d04"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 5", "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo when", combinedPassphrase);
        pieces = new String[]{
                "2:794880969:3:2:0:1:f4789d0747bfe6a371edfa1c2cae936aadc2003e47c1c04dc9affd9c6c7ec5cec6",
                "2:794880969:3:2:0:3:1de6ad816d840b4eb56caef55870cbb410a4701bbd84641ea5e10f9d6467d526e22"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 6", "legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth title", combinedPassphrase);
        pieces = new String[]{
                "2:329613522:3:2:0:1:9b4ec5d97931266d6d1c7340901f2ba0a060933ff5c70833b230e035bcb8697970",
                "2:329613522:3:2:0:3:d0eb508b6a927247465458c0af5c81e0e020b8bee054179a15919fa035283b6c20"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 7", "letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic bless", combinedPassphrase);
        pieces = new String[]{
                "2:650080011:3:2:0:1:1e6832ae3434850023fe10fed3e122096a6f416ec9acc1361db3c7c902d3dd206f3",
                "2:650080011:3:2:0:3:3b38980a9c9d8f006bfa32fc7ba3661c3f4dc44c5d0643a2591b575b087b97616a9"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 8", "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote", combinedPassphrase);
        pieces = new String[]{
                "2:416205772:3:2:0:1:d2ed4b7341576cc9c71b25e8d52994ff69",
                "2:416205772:3:2:0:3:18942824b9668b81ae4df467dfee154e423"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 9", "jelly better achieve collect unaware mountain thought cargo oxygen act hood bridge", combinedPassphrase);
        pieces = new String[]{
                "2:1450117018:3:2:0:1:14a7c2f8c7103db694dad81edd686e44c50b46cacad46e27d47",
                "2:1450117018:3:2:0:3:272ff55f2053f0e56d73103c38096719101cb2f8aba58c881b1"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 10", "renew stay biology evidence goat welcome casual join adapt armor shuffle fault little machine walk stumble urge swap", combinedPassphrase);
        pieces = new String[]{
                "2:1149643916:3:2:0:1:41061c70a8787519b92a3c01c6c4ca39968b6296ea4d253c93cc67af6982c98145",
                "2:1149643916:3:2:0:3:46ea293e8676f7e0577298699b6a69091175380db0a70be4fccd34557f8b497f9f"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 11", "dignity pass list indicate nasty swamp pool script soccer toe leaf photo multiply desk host tomato cradle drill spread actor shine dismiss champion exotic", combinedPassphrase);
        pieces = new String[]{
                "2:1986722977:3:2:0:1:5bcafe1f8254eda74d08130f36ebe7e12",
                "2:1986722977:3:2:0:3:874311737c3e304926551dc7d773e7c1e"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 12", "afford alter spike radar gate glance object seek swamp infant panel yellow", combinedPassphrase);
        pieces = new String[]{
                "2:342288316:3:2:0:1:823070117a782a99f9ddde6a1b299382f99e0ceb700b6bf84e",
                "2:342288316:3:2:0:3:a0a532bee3ad6a12d3454fef235c620f28a2a66ecc22350ac6"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 13", "indicate race push merry suffer human cruise dwarf pole review arch keep canvas theme poem divorce alter left", combinedPassphrase);
        pieces = new String[]{
                "2:937217762:3:2:0:1:5695a5aeb3c2be67ec18d0fef70d1bed840f1184fa347d52b2be003b105e2b82e3",
                "2:937217762:3:2:0:3:aab5117c36aa728949f36faf0c59b60478c77b14daeb9c3f7479bdfb1a48ba5c79"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 14", "clutch control vehicle tonight unusual clog visa ice plunge glimpse recipe series open hour vintage deposit universe tip job dress radar refuse motion taste", combinedPassphrase);
        pieces = new String[]{
                "2:819991570:3:2:0:1:13c4b2cd03c66a11053d72bc5ffab1bd04f",
                "2:819991570:3:2:0:3:1df0a2f0c44cd3f3697d77c49e89b7facd5"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 15", "turtle front uncle idea crush write shrug there lottery flower risk shell", combinedPassphrase);
        pieces = new String[]{
                "2:1167088846:3:2:0:1:c6812f3877ea4b1124e1ee7dad85397c8785d92b091930b600",
                "2:1167088846:3:2:0:3:15dfad3ac7979045a77552c00add8003f0afb2bb544803d31dc"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 16", "kiss carry display unusual confirm curtain upgrade antique rotate hello void custom frequent obey nut hole price segment", combinedPassphrase);
        pieces = new String[]{
                "2:164994364:3:2:0:1:6c42ae1932b4c8e77b9c879dcc472b324c1ab33c66f2dbb3ceed86ef074ed253dd",
                "2:164994364:3:2:0:3:a584b8d31b4358da4ca98cbc587d5172de26f372dddb0b33f69282ea4456f91d67"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 17", "exile ask congress lamp submit jacket era scheme attend cousin alcohol catch course end lucky hurt sentence oven short ball bird grab wing top", combinedPassphrase);
        pieces = new String[]{
                "2:1303792334:3:2:0:1:2a1186509404b0b0aea3fb4b8485c61498",
                "2:1303792334:3:2:0:3:4cde5f9dd178ed282b6f4dcf48383d1bb0"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 18", "board flee heavy tunnel powder denial science ski answer betray cargo cat", combinedPassphrase);
        pieces = new String[]{
                "2:1774004436:3:2:0:1:224b054403bb13b99b9fda82a58c695b1a3d6559c5b524c74b",
                "2:1774004436:3:2:0:3:359b4c1bd4139bc78c7833f1d875f09d77ca975345684b0dbd"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 19", "board blade invite damage undo sun mimic interest slam gaze truly inherit resist great inject rocket museum chief", combinedPassphrase);
        pieces = new String[]{
                "2:1530545541:3:2:0:1:1aa7fa2ebb02a1bf3c106ec54b95af3629cd223d3b4d302dae8f81623b5a6cbce2",
                "2:1530545541:3:2:0:3:2442e03305c5698f423962335dd60647b98447047322780616eae5202a7d3e0476"
        };
        combinedPassphrase = SecretSharingGenerator.combine(pieces);
        Assert.assertEquals("vector 20", "beyond stage sleep clip because twist token leaf atom beauty genius food business side grid unable middle armed observe pair crouch tonight away coconut", combinedPassphrase);
    }
}
