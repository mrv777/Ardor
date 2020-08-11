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

package nxt.crypto;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.math.GroupElement;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import nxt.Constants;
import nxt.util.Bip32Path;
import nxt.util.Convert;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Testing against the HDed25519.py sample from https://github.com/haimbender/orakolo based on https://github.com/LedgerHQ/orakolo/blob/master/papers/Ed25519_BIP%20Final.pdf
 * There is a known difference between the Java and Python code since in Java BigInteger.toByteArray uses two's compliment so can return 33 bytes while in Python int.to_bytes
 * always cuts the array to 32 byte. However it looks like the Java conversion is correct since without it some our key pairs does not work.
 */
public class KeyDerivationTest {

    private static final String MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    @Test
    public void pythonCompare() {
        byte[] skLeft = Convert.parseHexString("68454271c6ca2a78385e4dc687bf551ecd4f6a15b416e1b306c1024756351450");
        byte[] skRight = Convert.parseHexString("a0ed887d9b4e97a577d260acfc9f79aaaa7205560f103361b40f269d52e128e7");
        byte[] publicKey = Convert.parseHexString("03e89ec13912cf7af53937b74a2565b2d15b09e0ded57db5196f54894e0e30f9");
        byte[] chainCode = Convert.parseHexString("70bc1dcceaf19bf7525fc52fadcbbaf137f6a6dcbcde55c2205a6853e2af1f8b");
        KeyDerivation.Bip32Node node = new KeyDerivation.Bip32Node(skLeft, skRight, publicKey, chainCode, publicKey);
        KeyDerivation.Bip32Node childNode = KeyDerivation.deriveChildPrivateKey(node, 0);
        KeyDerivation.Bip32Node childPublicKey = KeyDerivation.deriveChildPublicKey(node, 0);
        Assert.assertArrayEquals(childNode.getMasterPublicKey(), childPublicKey.getMasterPublicKey());
    }

    @Test
    public void seedToParentPublicKey() {
        // Testing paths 42'/1/2 and 42'/3'/5
        String[][] paths = new String[][] {{"42'", "1/2"}, {"42'/3'", "5"}};

        String[] expectedPublicKeys = new String[] {
                "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee",
                "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c"
        };

        String[] expectedChainCodes = new String[] {
                "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36",
                "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984",
        };

        for (int i=0; i<paths.length; i++) {
            KeyDerivation.Bip32Node node = KeyDerivation.deriveMnemonic(paths[i][0], MNEMONIC);
            String[] childIndexes = paths[i][1].split("/");
            for (String index : childIndexes) {
                node = KeyDerivation.deriveChildPublicKey(node.getMasterPublicKey(), node.getChainCode(), Integer.parseInt(index));
            }
            Assert.assertEquals(expectedPublicKeys[i], Convert.toHexString(node.getMasterPublicKey()));
            Assert.assertEquals(expectedChainCodes[i], Convert.toHexString(node.getChainCode()));
        }
    }

    /**
     * Seed: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
     * Path: 42'/3'/5
     */
    @Test
    public void publicKeyDerivation() {
        byte[] parentPublicKey = Convert.parseHexString("0f18a624a1152b8d7e3c986f736586b4649c1ce5f74dc4cefef31be4a659b02f");
        byte[] chainCode = Convert.parseHexString("a251468a7abd759c80db7581c7c69a45b5b7b8171be394b8dce1f15395b742d7");
        KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveChildPublicKey(parentPublicKey, chainCode, 5);
        Assert.assertArrayEquals(Convert.parseHexString("286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c"), bip32Node.getMasterPublicKey());
        Assert.assertArrayEquals(Convert.parseHexString("7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984"), bip32Node.getChainCode());
    }

    /**
     * Seed: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
     * Path: 42'/3'/6
     */
    @Test
    public void publicKeyDerivation6() {
        byte[] parentPublicKey = Convert.parseHexString("0f18a624a1152b8d7e3c986f736586b4649c1ce5f74dc4cefef31be4a659b02f");
        byte[] chainCode = Convert.parseHexString("a251468a7abd759c80db7581c7c69a45b5b7b8171be394b8dce1f15395b742d7");
        KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveChildPublicKey(parentPublicKey, chainCode, 6);
        Assert.assertArrayEquals(Convert.parseHexString("b9ffd3a8e3b9bd8a34810699c8a0432261968a856c738ad1f0d183db77805d60"), bip32Node.getMasterPublicKey());
        Assert.assertArrayEquals(Convert.parseHexString("427e1d81d76d94bdf44eaf9e589149daf54502b8733edb5b70d0c0e53e72c28e"), bip32Node.getChainCode());
    }

    /**
     * Seed: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
     * Path: 42'/3'/7
     */
    @Test
    public void publicKeyDerivation7() {
        byte[] parentPublicKey = Convert.parseHexString("0f18a624a1152b8d7e3c986f736586b4649c1ce5f74dc4cefef31be4a659b02f");
        byte[] chainCode = Convert.parseHexString("a251468a7abd759c80db7581c7c69a45b5b7b8171be394b8dce1f15395b742d7");
        KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveChildPublicKey(parentPublicKey, chainCode, 7);
        Assert.assertArrayEquals(Convert.parseHexString("1a4d48da35ca00be11ec020f96be376941d345722801d17ff340b415c27379dc"), bip32Node.getMasterPublicKey());
        Assert.assertArrayEquals(Convert.parseHexString("f90be8aff110b83a938775afa2be0f089f0e94aa8e03b802d857576531b89196"), bip32Node.getChainCode());
    }

    /**
     * Seed: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
     * Path: 42'/1 then 42'/1/2
     */
    @Test
    public void iterativePublicKeyDerivation() {
        byte[] parentPublicKey = Convert.parseHexString("8ead9aaa894eef681f7dd550845c8b31f83334ee3035fd6577a6a2ec3230bc5e");
        byte[] parentChainCode = Convert.parseHexString("4e6eb1fa4860d9c7c8a784642412d7c55854a1bc07f32eedce44b3437c50bb32");

        // Derive 42'/1
        KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveChildPublicKey(parentPublicKey, parentChainCode, 1);
        byte[] childPublicKey = bip32Node.getMasterPublicKey();
        Assert.assertArrayEquals(Convert.parseHexString("b130227d51bd32f8bc67c7efaada3d1abc958e1771841efebb02d6df594b8f5a"), childPublicKey);
        byte[] childChainCode = bip32Node.getChainCode();
        Assert.assertArrayEquals(Convert.parseHexString("66c093dace29c82dcfcec8984bed3c1b515a1c8ca86b823d441ca5c40bcf3f37"), childChainCode);

        // Derive 42'/1/2 from 42'/1
        bip32Node = KeyDerivation.deriveChildPublicKey(childPublicKey, childChainCode, 2);
        byte[] grandChildPublicKey = bip32Node.getMasterPublicKey();
        Assert.assertArrayEquals(Convert.parseHexString("bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee"), grandChildPublicKey);
        byte[] grandChildChainCode = bip32Node.getChainCode();
        Assert.assertArrayEquals(Convert.parseHexString("c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36"), grandChildChainCode);
    }

    @Test
    public void privateKeyDerivation() {
        String[] paths = new String[] { "42'/1/2", "42'/3'/5"};

        String[][] expectedPaths = new String[][] {{
                "b02160bb753c495687eb0b0e0628bf637e85fd3aadac109847afa2ad20e69d41",
                "00ea111776aabeb85446b186110f8337a758681c96d5d01d5f42d34baf97087b",
                "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36",
                "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee"
        },{
                "78164270a17f697b57f172a7ac58cfbb95e007fdcd968c8c6a2468841fe69d41",
                "15c846a5d003f7017374d12105c25930a2bf8c386b7be3c470d8226f3cad8b6b",
                "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984",
                "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c"
        }};
        for (int i=0; i<paths.length; i++) {
            KeyDerivation.Bip32Node node = KeyDerivation.deriveMnemonic(paths[i], MNEMONIC);
            Assert.assertEquals(expectedPaths[i][0], Convert.toHexString(node.getPrivateKeyLeft()));
            Assert.assertEquals(expectedPaths[i][1], Convert.toHexString(node.getPrivateKeyRight()));
            Assert.assertEquals(expectedPaths[i][2], Convert.toHexString(node.getChainCode()));
            Assert.assertEquals(expectedPaths[i][3], Convert.toHexString(node.getMasterPublicKey()));
        }
    }

    /**
     * Here we make sure that deriving a public key from a node produces the same value as deriving the private key
     * and then taking the public key from the private key.
     * We test this with different seeds and paths.
     */
    @Test
    public void differentChildPublicKeyDerivation() {
        int[] pathArray = Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH.toPathArray();
        for (int i = 1; i < 5; i++) {
            pathArray[pathArray.length - 1] = i;
            String path = Bip32Path.bip32PathToStr(pathArray);
            KeyDerivation.Bip32Node node = KeyDerivation.deriveMnemonic(path, MNEMONIC + i);
            for (int j = 1; j < 10; j++) {
                KeyDerivation.Bip32Node derivedChildPublicKey = KeyDerivation.deriveChildPublicKey(node, j);
                KeyDerivation.Bip32Node derivedChildPrivateKey = KeyDerivation.deriveChildPrivateKey(node, j);
                Assert.assertEquals(Convert.toHexString(derivedChildPublicKey.getMasterPublicKey()), Convert.toHexString(derivedChildPrivateKey.getMasterPublicKey()));
            }
        }
    }

    /**
     * Same as differentChildPublicKeyDerivation but for many keys. Also serves as a speed test.
     */
    @Test
    public void speedTest() {
        long startTime = System.currentTimeMillis();
        KeyDerivation.Bip32Node node = KeyDerivation.deriveMnemonic(Constants.ARDOR_TESTNET_BIP32_ROOT_PATH.toString(), MNEMONIC);
        KeyDerivation.Bip32Node[] pkNodes = new KeyDerivation.Bip32Node[100];
        for (int i=0; i<100; i++) {
            pkNodes[i] = KeyDerivation.deriveChildPublicKey(node, i);
        }
        long measureTime = System.currentTimeMillis();
        System.out.printf("Public key generation interval: %d\n", measureTime - startTime);
        startTime = measureTime;

        KeyDerivation.Bip32Node[] skNodes = new KeyDerivation.Bip32Node[100];
        for (int i=0; i<100; i++) {
            skNodes[i] = KeyDerivation.deriveChildPrivateKey(node, i);
        }
        System.out.printf("Secret key generation interval: %d\n", System.currentTimeMillis() - startTime);

        for (int i=0; i<100; i++) {
            Assert.assertArrayEquals(pkNodes[i].getMasterPublicKey(), skNodes[i].getMasterPublicKey());
        }
    }

    /**
     * This is a full system test.
     * Derive private key (both curve25519 and ed25519 use the same) then derive public key using curve25519 and using
     * ed25519 and converting it to curve25519. We then make sure the keys are the same.
     * Next we make sure this key pair is a functional curve25519 key pair by using it for signing and verification.
     * Next we generate another key pair and test asymmetric encrypt/decrypt.
     * We repeat for different seeds, paths, and use each pair multiple times.
     */
    @Test
    public void deriveKeyPairsForSigningAndEncryption() {
        int[] path1 = Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH.toPathArray();
        int[] path2 = Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH.toPathArray();
        for (int i=0; i<1; i++) {
            System.out.println("i=" + i);
            for (int j=0; j<1; j++) {
                // Generate a path, node and private key
                path1[path1.length - 1] = j;
                KeyDerivation.Bip32Node node1 = KeyDerivation.deriveMnemonic(Bip32Path.bip32PathToStr(path1), MNEMONIC + i);
                byte[] privateKey1 = node1.getPrivateKeyLeft();

                // Use the ed25519 curve directly to generate a public key and make sure it is the same as the one derived from the node
                byte[] h = new byte[64];
                System.arraycopy(node1.getPrivateKeyLeft(),0, h, 0, 32);
                System.arraycopy(node1.getPrivateKeyRight(),0, h, 32, 32);
                EdDSAPrivateKeySpec edDSAPrivateKeySpec = new EdDSAPrivateKeySpec(EdDSANamedCurveTable.ED_25519_CURVE_SPEC, h);
                EdDSAPublicKeySpec edDSAPublicKeySpec = new EdDSAPublicKeySpec(edDSAPrivateKeySpec.getA(), EdDSANamedCurveTable.ED_25519_CURVE_SPEC);
                EdDSAPublicKey edDSAPublicKey = new EdDSAPublicKey(edDSAPublicKeySpec);
                Assert.assertArrayEquals(edDSAPublicKey.getAbyte(), node1.getMasterPublicKey());

                // Convert the ed25519 public key to curve25519 and make sure it is the same public key as the one generated directly
                // from curve25519
                byte[] publicKey1 = node1.getPublicKey();
                byte[] P = new byte[32];
                Curve25519.keygen(P, null, privateKey1);
                Assert.assertEquals(Convert.toHexString(P), Convert.toHexString(publicKey1));

                // Generate a second key pair
                path2[path2.length - 1] = 10000 + j;
                KeyDerivation.Bip32Node node2 = KeyDerivation.deriveMnemonic(Bip32Path.bip32PathToStr(path2), MNEMONIC + (10000 + i));
                byte[] privateKey2 = node2.getPrivateKeyLeft();
                byte[] publicKey2 = node2.getPublicKey();

                // Test that our key pairs are operational for signing and encryption to prove this is indeed a functional curve25519 key pair
                for (int k=0; k<1; k++) {
                    // Sign and Verify
                    byte[] messageBytes = ("message" + k).getBytes();
                    byte[] signature = Crypto.sign(messageBytes, privateKey1);
                    Assert.assertTrue(String.format("Verification failed %d,%d,%d", i, j, k), Crypto.verify(signature, messageBytes, publicKey1));

                    signature = Crypto.sign(messageBytes, privateKey2);
                    Assert.assertTrue(String.format("Verification failed %d,%d,%d", i, j, k), Crypto.verify(signature, messageBytes, publicKey2));

                    // Encrypt and Decrypt in both directions
                    byte[] bytes = ("MySecret" + k).getBytes();
                    EncryptedData encryptedData = EncryptedData.encrypt(bytes, privateKey1, publicKey2);
                    byte[] decryptedBytes = encryptedData.decrypt(privateKey2, publicKey1);
                    Assert.assertArrayEquals(String.format("Decryption failed %d,%d,%d", i, j, k), bytes, decryptedBytes);

                    encryptedData = EncryptedData.encrypt(bytes, privateKey2, publicKey1);
                    decryptedBytes = encryptedData.decrypt(privateKey1, publicKey2);
                    Assert.assertArrayEquals(String.format("Decryption failed %d,%d,%d", i, j, k), bytes, decryptedBytes);
                }
            }
        }
    }

    @Test
    public void mnemonicToSeed() {
        byte[] seed = KeyDerivation.mnemonicToSeed(MNEMONIC);
        Assert.assertEquals("5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",Convert.toHexString(seed));
    }

    @Test
    public void scalarMultiplication() {
        Assert.assertEquals("5866666666666666666666666666666666666666666666666666666666666666", Convert.toHexString(KeyDerivation.GENERATOR.toByteArray()));
        GroupElement point = KeyDerivation.GENERATOR.scalarMultiply(KeyDerivation.EIGHT_BYTES);
        Assert.assertEquals("b4b937fca95b2f1e93e41e62fc3c78818ff38a66096fad6e7973e5c90006d321", Convert.toHexString(point.toByteArray()));
        byte[] k = new byte[32];
        for (int i=0; i<32; i++) {
            k[i] = (byte) i;
        }
        point = KeyDerivation.GENERATOR.scalarMultiply(k);
        Assert.assertEquals("ca4a448c3fc4d04945da9fdf920976c05e9bbe3d8cebb1858ea44d587c5e63c3", Convert.toHexString(point.toByteArray()));
    }

    /**
     * Test the special cases of Java's conversion for BigInteger to byte array
     */
    @Test
    public void bigIntegerTwosComplement() {
        BigInteger twoPow255 = new BigInteger("2").pow(255);
        Assert.assertEquals(33, twoPow255.toByteArray().length);
        Assert.assertEquals(32, twoPow255.subtract(new BigInteger("1")).toByteArray().length);

        BigInteger twoPow247 = new BigInteger("2").pow(247);
        Assert.assertEquals(32, twoPow247.toByteArray().length);
        Assert.assertEquals(31, twoPow247.subtract(new BigInteger("1")).toByteArray().length);
    }

    @Test
    public void masterPublicKeySerialization() {
        String masterPublicKeyHex = "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee";
        String chainCodeHex = "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36";
        String expectedSerializedMasterPublicKey = masterPublicKeyHex + chainCodeHex + "a3a3ef64";
        byte[] masterPublicKey = Convert.parseHexString(masterPublicKeyHex);
        byte[] chainCode = Convert.parseHexString(chainCodeHex);
        byte[] serializedMasterPublicKey = new SerializedMasterPublicKey(masterPublicKey, chainCode).getSerializedMasterPublicKey();
        String result = Convert.toHexString(serializedMasterPublicKey);
        Assert.assertEquals(expectedSerializedMasterPublicKey, result);
        Assert.assertTrue(SerializedMasterPublicKey.isValidSerializedMasterPublicKey(Convert.parseHexString(result)));
    }

    @Test
    public void masterPublicKeyDeserialization() {
        String masterPublicKeyHex = "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee";
        String chainCodeHex = "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36";
        SerializedMasterPublicKey parsed = new SerializedMasterPublicKey(Convert.parseHexString(masterPublicKeyHex + chainCodeHex + "a3a3ef64"));
        SerializedMasterPublicKey built = new SerializedMasterPublicKey(Convert.parseHexString(masterPublicKeyHex), Convert.parseHexString(chainCodeHex));
        Assert.assertArrayEquals(parsed.getSerializedMasterPublicKey(), built.getSerializedMasterPublicKey());
    }

    @Test
    public void badMasterPublicKeyDeserialization() {
        String masterPublicKeyHex = "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee";
        String chainCodeHex = "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36";
        Assert.assertFalse(SerializedMasterPublicKey.isValidSerializedMasterPublicKey(Convert.parseHexString(masterPublicKeyHex + chainCodeHex)));
        Assert.assertFalse(SerializedMasterPublicKey.isValidSerializedMasterPublicKey(Convert.parseHexString(masterPublicKeyHex + chainCodeHex + "00000000")));
    }
}
