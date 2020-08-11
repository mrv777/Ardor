/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016-2020 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of this software, including this file, may be copied, modified,    *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

jQuery.t = function(text) {
    return text;
};

QUnit.module("nrs.key.derivation");

const MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

QUnit.test("mnemonicToSeed", function (assert) {
    let seed = KeyDerivation.mnemonicToSeed(MNEMONIC);
    assert.equal(converters.byteArrayToHexString(seed), "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4")
});

QUnit.test("scalarMultiplication", function (assert) {
    assert.equal(Ed25519.BASE_POINT.toHex(), "5866666666666666666666666666666666666666666666666666666666666666");
    let eight = BigInt(8);
    let eightBytes = converters.hexStringToByteArray(converters.bigIntToHexString(eight));
    let point = KeyDerivation.scalarMultiplicationCurve25519Base(eightBytes);
    assert.equal(point.toHex(), "b4b937fca95b2f1e93e41e62fc3c78818ff38a66096fad6e7973e5c90006d321");
    let k = new Array(32);
    for (let i=0; i<32; i++) {
        k[i] = i;
    }
    point = KeyDerivation.scalarMultiplicationCurve25519Base(k);
    assert.equal(point.toHex(), "ca4a448c3fc4d04945da9fdf920976c05e9bbe3d8cebb1858ea44d587c5e63c3");
});

QUnit.test("hmac", function (assert) {
    let b = converters.stringToByteArray("message");
    let c = converters.wordArrayToByteArray(CryptoJS.HmacSHA256(converters.byteArrayToWordArrayEx(b), "key"));
    assert.equal(converters.byteArrayToHexString(c), "6e9ef29b75fffc5b7abae527d58fdadb2fe42e7219011976917343065f58ed4a");

    let bytes = converters.hexStringToByteArray("0078aef631e246abc067648194216212074dbabafec9e0651c6b1dad0d1b7a92480c1b060cc7fa315a39c6de2d4b0001b00d014b4723d0c998c259238697cd96c60072410080");
    console.log(bytes);
    let chainCode = converters.hexStringToByteArray("3d610058aedf2f15d33c2fead966b493a869672c5445b703b7c141ef14398d8c");
    console.log(chainCode);
    c = converters.wordArrayToByteArrayImpl(CryptoJS.HmacSHA512(converters.byteArrayToWordArrayEx(bytes), converters.byteArrayToWordArrayEx(chainCode)));
    assert.deepEqual(c, converters.hexStringToByteArray("81a0d493d30b516a42d43a4e5ea0b037879fe410b827b45b0d3b4d665bf0cd772c71bc6f12569cc2f5fcd8e5da1d62a1b4b3289749ffcacc3a2f788e75962c59"));

    let obytes = [0,120,174,246,49,226,70,171,192,103,100,129,148,33,98,18,7,77,186,186,254,201,224,101,28,107,29,173,13,27,122,146,72,12,27,6,12,199,250,49,90,57,198,222,45,75,0,1,176,13,1,75,71,35,208,201,152,194,89,35,134,151,205,150,198,0,114,65,0,128];
    assert.deepEqual(obytes, bytes);
    let ochainCode = [61,97,0,88,174,223,47,21,211,60,47,234,217,102,180,147,168,105,103,44,84,69,183,3,183,193,65,239,20,57,141,140];
    c = converters.wordArrayToByteArrayImpl(CryptoJS.HmacSHA512(converters.byteArrayToWordArrayEx(bytes), converters.byteArrayToWordArrayEx(chainCode)));
    assert.deepEqual(c, converters.hexStringToByteArray("81a0d493d30b516a42d43a4e5ea0b037879fe410b827b45b0d3b4d665bf0cd772c71bc6f12569cc2f5fcd8e5da1d62a1b4b3289749ffcacc3a2f788e75962c59"));
});

/**
 * This test shows the difference between Java's two's compliment conversion from BigInteger to byte array vs.
 * Javascript's conversion as unsigned number. Compare to KeyDerivationTest.java bigIntegerTwosComplement() to understand
 * the difference.
 */
QUnit.test("bigIntegerTwosComplement", function (assert) {
    let twoPow256 = 2n ** 256n;
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow256)).length, 33);
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow256 - 1n)).length, 32);

    let twoPow255 = 2n ** 255n;
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow255)).length, 32);
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow255 - 1n)).length, 32);

    let twoPow248 = 2n ** 248n;
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow248)).length, 32);
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow248 - 1n)).length, 31);

    let twoPow247 = 2n ** 247n;
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow247)).length, 31);
    assert.equal(converters.hexStringToByteArray(converters.bigIntToHexString(twoPow247 - 1n)).length, 31);
});

QUnit.test("privateKeyDerivation", function (assert) {
    let paths = ["42'/1/2", "42'/3'/5"];

    let expectedPaths = [[
        "b02160bb753c495687eb0b0e0628bf637e85fd3aadac109847afa2ad20e69d41",
        "00ea111776aabeb85446b186110f8337a758681c96d5d01d5f42d34baf97087b",
        "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36",
        "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee"
    ],[
        "78164270a17f697b57f172a7ac58cfbb95e007fdcd968c8c6a2468841fe69d41",
        "15c846a5d003f7017374d12105c25930a2bf8c386b7be3c470d8226f3cad8b6b",
        "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984",
        "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c"
    ]];

    for (let i=0; i<paths.length; i++) {
        let node = KeyDerivation.deriveMnemonic(paths[i], MNEMONIC);
        assert.equal(converters.byteArrayToHexString(node.getPrivateKeyLeft()), expectedPaths[i][0]);
        assert.equal(converters.byteArrayToHexString(node.getPrivateKeyRight()), expectedPaths[i][1]);
        assert.equal(converters.byteArrayToHexString(node.getChainCode()), expectedPaths[i][2]);
        assert.equal(converters.byteArrayToHexString(node.getMasterPublicKey()), expectedPaths[i][3]);
    }

});

QUnit.test("seedToParentPublicKey", function (assert) {
    // Testing paths 42'/1/2 and 42'/3'/5
    let paths = [["42'", "1/2"], ["42'/3'", "5"]];

    let expectedPublicKeys = [
        "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee",
        "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c"
    ];

    let expectedChainCodes = [
        "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36",
        "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984",
    ];

    for (let i=0; i<paths.length; i++) {
        let node = KeyDerivation.deriveMnemonic(paths[i][0], MNEMONIC);
        let childIndexes = paths[i][1].split("/");
        for (let i=0; i<childIndexes.length; i++) {
            node = KeyDerivation.deriveChildPublicKeyFromNode(node, BigInt(childIndexes[i]));
        }
        assert.equal(converters.byteArrayToHexString(node.getMasterPublicKey()), expectedPublicKeys[i]);
        assert.equal(converters.byteArrayToHexString(node.getChainCode()), expectedChainCodes[i]);
    }
});

QUnit.test("publicKeyDerivation", function (assert) {
    let parentPublicKey = converters.hexStringToByteArray("0f18a624a1152b8d7e3c986f736586b4649c1ce5f74dc4cefef31be4a659b02f");
    let chainCode = converters.hexStringToByteArray("a251468a7abd759c80db7581c7c69a45b5b7b8171be394b8dce1f15395b742d7");
    let bip32Node = KeyDerivation.deriveChildPublicKeyFromParent(parentPublicKey, chainCode, 5);
    assert.equal(converters.byteArrayToHexString(bip32Node.getChainCode()), "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984");
    assert.equal(converters.byteArrayToHexString(bip32Node.getMasterPublicKey()), "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c");
});

QUnit.test("publicKeyDerivation6", function (assert) {
    let parentPublicKey = converters.hexStringToByteArray("0f18a624a1152b8d7e3c986f736586b4649c1ce5f74dc4cefef31be4a659b02f");
    let chainCode = converters.hexStringToByteArray("a251468a7abd759c80db7581c7c69a45b5b7b8171be394b8dce1f15395b742d7");
    let bip32Node = KeyDerivation.deriveChildPublicKeyFromParent(parentPublicKey, chainCode, 6);
    assert.equal(converters.byteArrayToHexString(bip32Node.getChainCode()), "427e1d81d76d94bdf44eaf9e589149daf54502b8733edb5b70d0c0e53e72c28e");
    assert.equal(converters.byteArrayToHexString(bip32Node.getMasterPublicKey()), "b9ffd3a8e3b9bd8a34810699c8a0432261968a856c738ad1f0d183db77805d60");
});

QUnit.test("publicKeyDerivation7", function (assert) {
    let parentPublicKey = converters.hexStringToByteArray("0f18a624a1152b8d7e3c986f736586b4649c1ce5f74dc4cefef31be4a659b02f");
    let chainCode = converters.hexStringToByteArray("a251468a7abd759c80db7581c7c69a45b5b7b8171be394b8dce1f15395b742d7");
    let bip32Node = KeyDerivation.deriveChildPublicKeyFromParent(parentPublicKey, chainCode, 7);
    assert.equal(converters.byteArrayToHexString(bip32Node.getChainCode()), "f90be8aff110b83a938775afa2be0f089f0e94aa8e03b802d857576531b89196");
    assert.equal(converters.byteArrayToHexString(bip32Node.getMasterPublicKey()), "1a4d48da35ca00be11ec020f96be376941d345722801d17ff340b415c27379dc");
});

QUnit.test("iterativePublicKeyDerivation", function (assert) {
    let parentPublicKey = converters.hexStringToByteArray("8ead9aaa894eef681f7dd550845c8b31f83334ee3035fd6577a6a2ec3230bc5e");
    let parentChainCode = converters.hexStringToByteArray("4e6eb1fa4860d9c7c8a784642412d7c55854a1bc07f32eedce44b3437c50bb32");

    // Derive 42'/1
    let bip32Node = KeyDerivation.deriveChildPublicKeyFromParent(parentPublicKey, parentChainCode, 1);
    let childPublicKey = bip32Node.getMasterPublicKey();
    assert.equal(converters.byteArrayToHexString(childPublicKey), "b130227d51bd32f8bc67c7efaada3d1abc958e1771841efebb02d6df594b8f5a");
    let childChainCode = bip32Node.getChainCode();
    assert.equal(converters.byteArrayToHexString(childChainCode), "66c093dace29c82dcfcec8984bed3c1b515a1c8ca86b823d441ca5c40bcf3f37");

    // Derive 42'/1/2 from 42'/1
    bip32Node = KeyDerivation.deriveChildPublicKeyFromParent(childPublicKey, childChainCode, 2);
    let grandChildPublicKey = bip32Node.getMasterPublicKey();
    assert.equal(converters.byteArrayToHexString(grandChildPublicKey), "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee");
    let grandChildChainCode = bip32Node.getChainCode();
    assert.equal(converters.byteArrayToHexString(grandChildChainCode), "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36");
});

QUnit.test("differentChildPublicKeyDerivation", function (assert) {
    for (let i = 1; i < 3; i++) {
        let node = KeyDerivation.deriveMnemonic("m/44'/16754'/0/1", MNEMONIC + i);
        for (let j = 1; j < 3; j++) {
            let derivedChildPublicKey = KeyDerivation.deriveChildPublicKeyFromNode(node, j);
            let derivedChildPrivateKey = KeyDerivation.deriveChildPrivateKey(node, j);
            assert.equal(converters.byteArrayToHexString(derivedChildPublicKey.getMasterPublicKey()), converters.byteArrayToHexString(derivedChildPrivateKey.getMasterPublicKey()));
        }
    }
});

QUnit.test("deriveKeyPairsForSigningAndEncryption", function (assert) {
    for (let i=0; i<2; i++) {
        for (let j=0; j<2; j++) {
            // Generate a path, node and private key
            let path1 = "m/44'/16754'/0/1/" + j;
            let node1 = KeyDerivation.deriveMnemonic(path1, MNEMONIC + i);
            let privateKey1 = node1.getPrivateKeyLeft();
            console.log(`privateKey1: ${converters.byteArrayToHexString(privateKey1)}`);
            let publicKey1 = node1.getPublicKey();
            console.log(`publicKey1: ${converters.byteArrayToHexString(publicKey1)}`);

            let path2 = "m/44'/16754'/0/1/" + (10000 + j);
            let node2 = KeyDerivation.deriveMnemonic(path2, MNEMONIC + (10000 + i));
            let privateKey2 = node2.getPrivateKeyLeft();
            console.log(`publicKey2: ${converters.byteArrayToHexString(privateKey2)}`);
            let publicKey2 = node2.getPublicKey();
            console.log(`publicKey2: ${converters.byteArrayToHexString(publicKey2)}`);

            // Test that our key pairs are operational for signing and encryption to prove this is indeed a functional curve25519 key pair
            let nonce = new Array(32);
            for (let l=0; l<nonce.length; l++) {
                nonce[i] = i;
            }
            for (let k=0; k<2; k++) {
                // Sign and Verify
                let messageBytes = converters.stringToByteArray("message" + k);
                let signature = NRS.signBytesWithPrivateKey(messageBytes, privateKey1);

                assert.ok(NRS.verifySignature(signature, converters.byteArrayToHexString(messageBytes), converters.byteArrayToHexString(publicKey1)));
                signature = NRS.signBytesWithPrivateKey(messageBytes, privateKey2);

                assert.ok(NRS.verifySignature(signature, converters.byteArrayToHexString(messageBytes), converters.byteArrayToHexString(publicKey2)));
                // Encrypt and Decrypt in both directions
                let mySecret = NRS.pkcs7Pad(new Uint8Array(converters.stringToByteArray("0123456789ABCDEF")));
                let cipherBytes = NRS.aesEncrypt(mySecret, { privateKey: privateKey1, publicKey: publicKey2, nonce: nonce });
                let data = NRS.aesDecrypt(cipherBytes, { privateKey: privateKey2, publicKey: publicKey1, nonce: nonce });
                assert.equal(converters.byteArrayToString(data.decrypted), mySecret);

                cipherBytes = NRS.aesEncrypt(mySecret, { privateKey: privateKey2, publicKey: publicKey1, nonce: nonce });
                data = NRS.aesDecrypt(cipherBytes, { privateKey: privateKey1, publicKey: publicKey2, nonce: nonce });
                assert.equal(converters.byteArrayToString(data.decrypted), mySecret);
            }
        }
    }
});

QUnit.test("serializedMasterPublicKey", function (assert) {
    const masterPublicKeyHex = "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee";
    const chainCodeHex = "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36";
    const expectedChecksum = "a3a3ef64";
    const serializedMasterPublicKey = KeyDerivation.computeSerializedMasterPublicKey(
        converters.hexStringToByteArray(masterPublicKeyHex), converters.hexStringToByteArray(chainCodeHex));
    assert.equal(converters.byteArrayToHexString(serializedMasterPublicKey),
        masterPublicKeyHex + chainCodeHex + expectedChecksum, "serialization");
    assert.ok(KeyDerivation.isValidSerializedMasterPublicKey(serializedMasterPublicKey), "is valid");
    assert.notOk(KeyDerivation.isValidSerializedMasterPublicKey(converters.hexStringToByteArray(masterPublicKeyHex + chainCodeHex + "00000000")), "is not valid");
});