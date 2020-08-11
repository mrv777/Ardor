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
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;
import java.util.stream.Stream;

public class BIP39Test {
    public static class TestVector {
        private final BigInteger entropy;
        private final int numberOfEntropyBits;
        private final String[] mnemonic;

        private TestVector(String entropy, String mnemonic) {
            this.entropy = new BigInteger(entropy, 16);
            numberOfEntropyBits = entropy.length() * 4;
            this.mnemonic = mnemonic.split(" ");
        }

        public BigInteger getEntropy() {
            return entropy;
        }

        public String[] getMnemonic() {
            return mnemonic;
        }
    }

    public static final TestVector[] testVectors = new TestVector[]{
            new TestVector("00000000000000000000000000000000","abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"),
            new TestVector("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f","legal winner thank year wave sausage worth useful legal winner thank yellow"),
            new TestVector("80808080808080808080808080808080","letter advice cage absurd amount doctor acoustic avoid letter advice cage above"),
            new TestVector("ffffffffffffffffffffffffffffffff","zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"),
            new TestVector("000000000000000000000000000000000000000000000000","abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon agent"),
            new TestVector("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f","legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth useful legal will"),
            new TestVector("808080808080808080808080808080808080808080808080","letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter always"),
            new TestVector("ffffffffffffffffffffffffffffffffffffffffffffffff","zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo when"),
            new TestVector("0000000000000000000000000000000000000000000000000000000000000000","abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art"),
            new TestVector("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f","legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth title"),
            new TestVector("8080808080808080808080808080808080808080808080808080808080808080","letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic bless"),
            new TestVector("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff","zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote"),
            new TestVector("77c2b00716cec7213839159e404db50d","jelly better achieve collect unaware mountain thought cargo oxygen act hood bridge"),
            new TestVector("b63a9c59a6e641f288ebc103017f1da9f8290b3da6bdef7b","renew stay biology evidence goat welcome casual join adapt armor shuffle fault little machine walk stumble urge swap"),
            new TestVector("3e141609b97933b66a060dcddc71fad1d91677db872031e85f4c015c5e7e8982","dignity pass list indicate nasty swamp pool script soccer toe leaf photo multiply desk host tomato cradle drill spread actor shine dismiss champion exotic"),
            new TestVector("0460ef47585604c5660618db2e6a7e7f","afford alter spike radar gate glance object seek swamp infant panel yellow"),
            new TestVector("72f60ebac5dd8add8d2a25a797102c3ce21bc029c200076f","indicate race push merry suffer human cruise dwarf pole review arch keep canvas theme poem divorce alter left"),
            new TestVector("2c85efc7f24ee4573d2b81a6ec66cee209b2dcbd09d8eddc51e0215b0b68e416","clutch control vehicle tonight unusual clog visa ice plunge glimpse recipe series open hour vintage deposit universe tip job dress radar refuse motion taste"),
            new TestVector("eaebabb2383351fd31d703840b32e9e2","turtle front uncle idea crush write shrug there lottery flower risk shell"),
            new TestVector("7ac45cfe7722ee6c7ba84fbc2d5bd61b45cb2fe5eb65aa78","kiss carry display unusual confirm curtain upgrade antique rotate hello void custom frequent obey nut hole price segment"),
            new TestVector("4fa1a8bc3e6d80ee1316050e862c1812031493212b7ec3f3bb1b08f168cabeef","exile ask congress lamp submit jacket era scheme attend cousin alcohol catch course end lucky hurt sentence oven short ball bird grab wing top"),
            new TestVector("18ab19a9f54a9274f03e5209a2ac8a91","board flee heavy tunnel powder denial science ski answer betray cargo cat"),
            new TestVector("18a2e1d81b8ecfb2a333adcb0c17a5b9eb76cc5d05db91a4","board blade invite damage undo sun mimic interest slam gaze truly inherit resist great inject rocket museum chief"),
            new TestVector("15da872c95a13dd738fbf50e427583ad61f18fd99f628c417a61cf8343c90419","beyond stage sleep clip because twist token leaf atom beauty genius food business side grid unable middle armed observe pair crouch tonight away coconut")
    };

    @Test
    public void mnemonicToEntropyTest() {
        for (int i = 0; i < testVectors.length; i++) {
            TestVector testVector = testVectors[i];
            Assert.assertEquals("mnemonicToEntropy vector " + i,
                    testVector.entropy,
                    BIP39.mnemonicToEntropy(testVector.mnemonic));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void badWord() {
        BIP39.mnemonicToEntropy("able able able able able able able able able able able alan".split(" "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badWordCount11() {
        BIP39.mnemonicToEntropy(Stream.generate(() -> "able").limit(11).toArray(String[]::new));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badWordCount13() {
        BIP39.mnemonicToEntropy(Stream.generate(() -> "able").limit(13).toArray(String[]::new));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badWordCount27() {
        BIP39.mnemonicToEntropy(Stream.generate(() -> "able").limit(27).toArray(String[]::new));
    }

    @Test
    public void entropyToMnemonicTest() {
        for (int i = 0; i < testVectors.length; i++) {
            TestVector testVector = testVectors[i];
            Assert.assertArrayEquals("entropyToMnemonic vector " + i,
                    testVector.mnemonic,
                    BIP39.entropyToMnemonic(testVector.entropy, testVector.numberOfEntropyBits));
        }
    }

    @Test
    public void randomBytesToMnemonicAndBack() {
        Random r = new Random();
        for (int i=0; i<10000; i++) {
            int bits = 128 + 32 * r.nextInt(5);
            byte[] randomBytes = new byte[bits/8];
            r.nextBytes(randomBytes);
            BigInteger entropy = new BigInteger(Convert.toHexString(randomBytes), 16);
            String[] mnemonic = BIP39.entropyToMnemonic(entropy, bits);
            BigInteger reproducedEntropy = BIP39.mnemonicToEntropy(mnemonic);
            Assert.assertEquals(entropy, reproducedEntropy);
        }
    }
}
