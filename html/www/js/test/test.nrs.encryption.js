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

QUnit.module("nrs.encryption");

QUnit.test("generatePublicKey", function (assert) {
    assert.throws(function() { NRS.generatePublicKey("") }, { message: "Can't generate public key without the user's password." }, "empty.public.key");
    assert.equal(NRS.generatePublicKey(NRS.getPrivateKey("12345678")), "a65ae5bc3cdaa9a0dd66f2a87459bbf663140060e99ae5d4dfe4dbef561fdd37", "public.key");
    assert.equal(NRS.generatePublicKey(NRS.getPrivateKey("hope peace happen touch easy pretend worthless talk them indeed wheel state")), "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b", "public.key");
});

QUnit.test("getPublicKey", function (assert) {
    var publicKey1 = NRS.getPublicKeyFromSecretPhrase("12345678");
    assert.equal(publicKey1, "a65ae5bc3cdaa9a0dd66f2a87459bbf663140060e99ae5d4dfe4dbef561fdd37", "public.key");
});

QUnit.test("getAccountIdFromPublicKey", function (assert) {
    assert.equal(NRS.getAccountIdFromPublicKey("112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b", true), "ARDOR-XK4R-7VJU-6EQG-7R335", "account.rs");
    assert.equal(NRS.getAccountIdFromPublicKey("112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b", false), "5873880488492319831", "account.rs");
});

QUnit.test("getPrivateKey", function (assert) {
    assert.equal(NRS.getPrivateKey("12345678"), "e8797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f", "private.key");
});

QUnit.test("encryptDecryptNote", async function (assert) {
    var senderSecretPhrase = "rshw9abtpsa2";
    var senderPublicKeyHex = NRS.getPublicKeyFromSecretPhrase(senderSecretPhrase);
    var recipientSecretPhrase = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    var receiverPublicKeyHex = NRS.getPublicKeyFromSecretPhrase(recipientSecretPhrase);
    var encryptedNote = await NRS.encryptNote("MyMessage", {
        publicKey: receiverPublicKeyHex,
        privateKey: NRS.getPrivateKey(senderSecretPhrase)
    });
    assert.equal(encryptedNote.message.length, 96, "message.length");
    assert.equal(encryptedNote.nonce.length, 64, "nonce.length");
    var decryptedNote = await NRS.decryptNote(encryptedNote.message, {
        nonce: encryptedNote.nonce,
        publicKey: senderPublicKeyHex,
        privateKey: NRS.getPrivateKey(recipientSecretPhrase)
    });
    assert.equal(decryptedNote.message, "MyMessage", "decrypted");
});

QUnit.test("encryptDecryptData", function (assert) {
    var senderPassphrase = "rshw9abtpsa2";
    var senderPublicKeyHex = NRS.getPublicKeyFromSecretPhrase(senderPassphrase);
    var senderPrivateKeyHex = NRS.getPrivateKey(senderPassphrase);
    var receiverPassphrase = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    var receiverPublicKeyHex = NRS.getPublicKeyFromSecretPhrase(receiverPassphrase);
    var receiverPrivateKeyHex = NRS.getPrivateKey(receiverPassphrase);
    var encryptedData = NRS.encryptDataRoof(converters.stringToByteArray("MyMessage"), {
        privateKey: converters.hexStringToByteArray(senderPrivateKeyHex),
        publicKey: converters.hexStringToByteArray(receiverPublicKeyHex)
    });
    assert.equal(encryptedData.data.length, 48, "message.length");
    assert.equal(encryptedData.nonce.length, 32, "nonce.length");
    var decryptedData = NRS.decryptDataRoof(encryptedData.data, {
        nonce: encryptedData.nonce,
        privateKey: converters.hexStringToByteArray(receiverPrivateKeyHex),
        publicKey: converters.hexStringToByteArray(senderPublicKeyHex)
    });
    assert.equal(decryptedData.message, "MyMessage", "decrypted");
    assert.equal(decryptedData.sharedKey.length, 64, "sharedKey");
});

// Based on testnet transaction 17867212180997536482
QUnit.test("getSharedKey", function (assert) {
    var privateKey = NRS.getPrivateKey("rshw9abtpsa2");
    var publicKey = "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b";
    var nonce = "67c2be503505d8e6498cd108a5f37c624899dcdae025276d720f608e54cf3177";
    var nonceBytes = converters.hexStringToByteArray(nonce);
    var sharedKeyBytes = NRS.getSharedKey(converters.hexStringToByteArray(privateKey), converters.hexStringToByteArray(publicKey), nonceBytes);
    // Make sure it's the same key produced by the server getSharedKey API
    assert.equal(converters.byteArrayToHexString(sharedKeyBytes), "68dd970a1144cc7595c745541b0318b08aa6ccd8121e061b378fc27ffc5e1cd1");
    var options = {};
    options.sharedKey = sharedKeyBytes;
    var encryptedMessage = "8adee4dee3e3311a631a29553140d177932cf0743c05846d897b24545d6839cbf368fc0b0eec628bfd69e95d006e3eb8";
    var decryptedMessage = NRS.decryptDataRoof(converters.hexStringToByteArray(encryptedMessage), options);
    assert.equal(decryptedMessage.message, "hello world");
    assert.equal(decryptedMessage.sharedKey, converters.byteArrayToHexString(sharedKeyBytes));
});

// Based on testnet transaction 2376600560388810797
QUnit.test("decryptCompressedText", function (assert) {
    var privateKey = NRS.getPrivateKey("rshw9abtpsa2");
    var publicKey = "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b";
    var nonce = "ca627f0252c6ca080067deedbed48f0a651789314fcbe8547815becce1d93cdc";
    var options = {
        privateKey: converters.hexStringToByteArray(privateKey),
        publicKey: converters.hexStringToByteArray(publicKey),
        nonce: converters.hexStringToByteArray(nonce)
    };
    var encryptedMessage = "a1b84c964ca98e0b2a57587c67286caf245d637c18f28938cf38544972dd30ccd3551db86cfceda21b750df076dce267";
    var decryptedMessage = NRS.decryptDataRoof(converters.hexStringToByteArray(encryptedMessage), options);
    assert.equal(decryptedMessage.message, "hello world");
});

// Based on testnet transaction 12445814829537070352
QUnit.test("decryptUncompressedText", function (assert) {
    var privateKey = NRS.getPrivateKey("rshw9abtpsa2");
    var publicKey = "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b";
    var nonce = "c9a707dbfab3d4b8188f6ee4e884fee459f39e1b45f7d7e8ee8ae1100be18854";
    var options = {
        privateKey: converters.hexStringToByteArray(privateKey),
        publicKey: converters.hexStringToByteArray(publicKey),
        nonce: converters.hexStringToByteArray(nonce),
        isCompressed: false
    };
    var encryptedMessage = "c0d97e7261a604f106ec3a17d1a650ef500747bab10e60f94958d1da6689abe9";
    var decryptedMessage = NRS.decryptDataRoof(converters.hexStringToByteArray(encryptedMessage), options);
    assert.equal(decryptedMessage.message.substring(0, 11), "hello world"); // messages less than 16 bytes long does not decrypt correctly
});

// Based on testnet transaction 16098450341097007976
QUnit.test("decryptCompressedBinary", function (assert) {
    var privateKey = NRS.getPrivateKey("rshw9abtpsa2");
    var publicKey = "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b";
    var nonce = "704227105cf3701e2c4e581e43cc4266e1230474443f777d43c75bf4454f0782";
    var options = {
        privateKey: converters.hexStringToByteArray(privateKey),
        publicKey: converters.hexStringToByteArray(publicKey),
        nonce: converters.hexStringToByteArray(nonce),
        isCompressed: true,
        isText: false
    };
    var encryptedMessage = "6c6dbf1aaa0ff170df9d0f15785e9d956d6c1e860288916c7a7651dfcc3b81678b6fb7afd667a2a4e759ea96e615a8ab";
    var decryptedMessage = NRS.decryptDataRoof(converters.hexStringToByteArray(encryptedMessage), options);
    assert.equal(converters.byteArrayToString(converters.hexStringToByteArray(decryptedMessage.message)), "hello world");
});

// Based on testnet transaction 15981469747709703862
QUnit.test("decryptUncompressedBinary", function (assert) {
    var privateKey = NRS.getPrivateKey("rshw9abtpsa2");
    var publicKey = "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b";
    var nonce = "bf8a1aa744dd7c95f10efca5cb55f6275f59816357307faab8faac8bf96ec822";
    var options = {
        privateKey: converters.hexStringToByteArray(privateKey),
        publicKey: converters.hexStringToByteArray(publicKey),
        nonce: converters.hexStringToByteArray(nonce),
        isCompressed: false,
        isText: false
    };
    var encryptedMessage = "5e8b0414de46cdb34ac0ad6dd6a30cd31524f5f8c65ae7c1c0fcd463973aa6df";
    var decryptedMessage = NRS.decryptDataRoof(converters.hexStringToByteArray(encryptedMessage), options);
    assert.equal(converters.byteArrayToString(converters.hexStringToByteArray(decryptedMessage.message)).substring(0, 11), "hello world");
});

QUnit.test("signAndVerify", function(assert) {
    var message = "0200000001000169fc25000f00584486d2ba4dbd7eaeadd071f9f8c3593cee620e1e374033551147d68899b5296589cd7b584899c8000000000000000020a107000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000684200009e0c8cc5fe930863080000000100be3eea9a483308cb3134ce068e77b56e7c25af19480742880179827cb3c9b5c0000000000000000000000000000000000000000000000000000000000000000000000000";
    var secretPhrase = "356869696739425064596f427a576e693051506143446e6f36577a305667386f5839794d6358526a45686d6b75514b687642";
    var signature = NRS.signBytes(message, NRS.getPrivateKey(converters.hexStringToString(secretPhrase)));
    assert.equal(signature, "aad6cd4bfe73eb39d42f549bce8f14b80a49e12cfebc0a9ce362876deb50c40609583f9b4e8fbbc45270f63947b1102457b10f9127f731f7a96891b8fdd8441f", "sign.message");
    var publicKey = "584486d2ba4dbd7eaeadd071f9f8c3593cee620e1e374033551147d68899b529";
    assert.equal(NRS.verifySignature(signature, message, publicKey), true, "verify.signature");
});

QUnit.test("shamirSecretSharingWikipediaExample", function(assert) {
    const prime = bigInt("1613");
    const secret = bigInt("1234");
    const allShares = sss.split(secret, 3, 5, prime);

    // Works with 3 shares
    var shares1 = [];
    shares1.push(allShares[0], allShares[2], allShares[3]);
    var reproducedSecret = sss.combine(shares1, prime);
    assert.equal(secret.compareTo(reproducedSecret), 0);

    // Works with other 3 shares
    var shares2 = [];
    shares2.push(allShares[0], allShares[1], allShares[4]);
    reproducedSecret = sss.combine(shares2, prime);
    assert.equal(secret.compareTo(reproducedSecret), 0);

    // Fails with only 2 shares
    var shares3 = [];
    shares3.push(allShares[1], allShares[4]);
    reproducedSecret = sss.combine(shares3, prime);
    assert.notEqual(secret.compareTo(reproducedSecret), 0);
});

QUnit.test("shamirSecretSharingSplitAndCombine", function(assert) {
    var secret = "298106192037605529109565170145082624171";
    var secretAs128Bit = bigInt(secret);
    var split = sss.split(secretAs128Bit, 3, 5, sss.PRIME_4096_BIT);
    split.forEach(function(piece) {
        assert.ok(piece.share.compareTo(bigInt.zero) > 0 && piece.share.compareTo(sss.PRIME_4096_BIT) < 0);
    });
    var shares = [split[0], split[2], split[4]];
    var secretPhrase = sss.combine(shares, sss.PRIME_4096_BIT);
    assert.equal(secretPhrase, secret);
});

const ALICE_SECRET_PHRASE = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
const BOB_SECRET_PHRASE = "rshw9abtpsa2";
const CHUCK_SECRET_PHRASE = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
const DAVE_SECRET_PHRASE = "t9G2ymCmDsQij7VtYinqrbGCOAtDDA3WiNr";
const RIKER_SECRET_PHRASE = "5hiig9BPdYoBzWni0QPaCDno6Wz0Vg8oX9yMcXRjEhmkuQKhvB";

QUnit.test("wordsTo128bitNumberAndBack", function(assert) {
    const secretInteger = "298106192037605529109565170145082624171";
    const secret = sss.VERSIONS.LEGACY_WORDS.secretToNumber(ALICE_SECRET_PHRASE);
    assert.equal(secret, secretInteger);
    const reproducedSecret = sss.VERSIONS.LEGACY_WORDS.numberToSecret(secret);
    assert.equal(reproducedSecret, ALICE_SECRET_PHRASE);
});

QUnit.test("splitAndCombine12wordsSecretPhrase", function(assert) {
    // Generate the pieces
    let pieces = sss.splitPhrase(ALICE_SECRET_PHRASE, 5, 3, bigInt.zero);
    assert.equal("1", pieces[0][0], "generated version of the pieces");

    // Select pieces and combine
    var selectedPieces = [pieces[0], pieces[2], pieces[4]];
    var combinedSecret = sss.combineSecret(selectedPieces);
    assert.equal(combinedSecret, ALICE_SECRET_PHRASE);

    // Select pieces and combine
    selectedPieces = [pieces[1], pieces[3], pieces[4]];
    combinedSecret = sss.combineSecret(selectedPieces);
    assert.equal(combinedSecret, ALICE_SECRET_PHRASE);

    // Again with 2 out of 3
    pieces = sss.splitPhrase(ALICE_SECRET_PHRASE, 3, 2, bigInt.zero);
    selectedPieces = [pieces[0], pieces[2]];
    combinedSecret = sss.combineSecret(selectedPieces);
    assert.equal(combinedSecret, ALICE_SECRET_PHRASE);
});

QUnit.test("splitAndCombineRandomSecretPhrase", function(assert) {
    function test(secretPhrase) {
        // Generate the pieces
        const pieces = sss.splitPhrase(secretPhrase, 7, 4, bigInt.zero);

        // Select pieces and combine
        let selectedPieces = [pieces[1], pieces[3], pieces[5], pieces[6]];
        let combinedSecret = sss.combineSecret(selectedPieces);
        assert.equal(combinedSecret, secretPhrase);

        // Select pieces and combine
        selectedPieces = [pieces[1], pieces[2], pieces[4], pieces[6]];
        combinedSecret = sss.combineSecret(selectedPieces);
        assert.equal(combinedSecret, secretPhrase);
    }

    test(BOB_SECRET_PHRASE);
    test(CHUCK_SECRET_PHRASE);
    test(DAVE_SECRET_PHRASE);
    test(RIKER_SECRET_PHRASE);
});

QUnit.test("splitAndCombineShortRandomSecretPhrase", function(assert) {
    // Generate the pieces
    const pieces = sss.splitPhrase("aaa", 7, 4, bigInt.zero);
    assert.equal("0", pieces[0][0], "generated version of the pieces");

    // Select pieces and combine
    var selectedPieces = [pieces[1], pieces[3], pieces[5], pieces[6]];
    var combinedSecret = sss.combineSecret(selectedPieces);
    assert.equal(combinedSecret, "aaa");
});

QUnit.test("splitValidityChecks", function(assert) {
    try {
        sss.splitPhrase(ALICE_SECRET_PHRASE, 4, 1, bigInt.zero);
        assert.fail();
    } catch (e) {
        assert.equal(true, e instanceof Error);
    }
    try {
        sss.splitPhrase(ALICE_SECRET_PHRASE, 4, 5, bigInt.zero);
        assert.fail();
    } catch (e) {
        assert.equal(true, e instanceof Error);
    }
});

QUnit.test("splitAndCombineBIP39Mnemonics", function(assert) {
    englishTestVectors.filter(vector => !new bigInt(vector[0], 16).isZero())
        .forEach((vector, index) => {
            // Generate the pieces
            let pieces = sss.splitPhrase(vector[1], 5, 3, bigInt.zero);
            assert.equal("2", pieces[0][0], "generated version of the pieces, vector " + index);

            // Select pieces and combine
            let selectedPieces = [pieces[0], pieces[2], pieces[4]];
            let combinedSecret = sss.combineSecret(selectedPieces);
            assert.equal(combinedSecret, vector[1], "3of5, pieces 0,2,4 for vector " + index);

            // Select pieces and combine
            selectedPieces = [pieces[1], pieces[3], pieces[4]];
            combinedSecret = sss.combineSecret(selectedPieces);
            assert.equal(combinedSecret, vector[1], "3of5, pieces 1,3,4 for vector " + index);

            // Again with 2 out of 3
            pieces = sss.splitPhrase(vector[1], 3, 2, bigInt.zero);
            selectedPieces = [pieces[0], pieces[2]];
            combinedSecret = sss.combineSecret(selectedPieces);
            assert.equal(combinedSecret, vector[1], "2of3, pieces 0,2 for vector " + index);
        });
});

QUnit.test("splitAndCombineCustomStringWithSpace", function(assert) {
    // Generate the pieces
    const secretPhrase = "lorem ipsum";
    const pieces = sss.splitPhrase(secretPhrase, 7, 4, bigInt.zero);
    assert.equal("0", pieces[0][0], "generated version of the pieces");

    // Select pieces and combine
    let selectedPieces = [pieces[1], pieces[3], pieces[5], pieces[6]];
    let combinedSecret = sss.combineSecret(selectedPieces);
    assert.equal(secretPhrase, combinedSecret);

    // Select pieces and combine
    selectedPieces = [pieces[1], pieces[2], pieces[4], pieces[6]];
    combinedSecret = sss.combineSecret(selectedPieces);
    assert.equal(secretPhrase, combinedSecret);
});

QUnit.test("splitAndCombinePrivateKey", function(assert) {
    function splitAndCombinePrivateKey(privateKey) {
        // Generate the pieces
        let pieces = sss.splitPrivateKey(privateKey, 5, 3, bigInt.zero);
        assert.equal("3", pieces[0][0], "generated version of the pieces");
        assert.ok(sss.isPrivateKeySecret(pieces), "isPrivateKeySecret?");

        // Select pieces and combine
        let selectedPieces = [pieces[0], pieces[2], pieces[4]];
        let combinedSecret = sss.combineSecret(selectedPieces);
        assert.equal(combinedSecret, privateKey, "3of5, pieces 0,2,4");

        // Select pieces and combine
        selectedPieces = [pieces[1], pieces[3], pieces[4]];
        combinedSecret = sss.combineSecret(selectedPieces);
        assert.equal(combinedSecret, privateKey, "3of5, pieces 1,3,4");

        // Again with 2 out of 3
        pieces = sss.splitPrivateKey(privateKey, 3, 2, bigInt.zero);
        selectedPieces = [pieces[0], pieces[2]];
        combinedSecret = sss.combineSecret(selectedPieces);
        assert.equal(combinedSecret, privateKey, "2of3, pieces 0,2");
    }

    splitAndCombinePrivateKey(NRS.getPrivateKey(ALICE_SECRET_PHRASE));
    splitAndCombinePrivateKey(NRS.getPrivateKey(BOB_SECRET_PHRASE));
    splitAndCombinePrivateKey(NRS.getPrivateKey(CHUCK_SECRET_PHRASE));
    splitAndCombinePrivateKey(NRS.getPrivateKey(DAVE_SECRET_PHRASE));
    splitAndCombinePrivateKey(NRS.getPrivateKey(RIKER_SECRET_PHRASE));
});

QUnit.test('combineFromServerShares', function(assert) {
    let pieces = [
        "0:-1797511508:5:3:0:1:00994e4109d68fba8ed92c1b65b7c50963d4480f623de5d5d1f230eb199ac37c088f3bc3",
        "0:-1797511508:5:3:0:2:01135524eff764421052eb34520b6f009588ad5bb3f707443dacccab51b430283513ee4a",
        "0:-1797511508:5:3:0:5:042599fb5a1d4876b15b8e1705864273d5395e7f05f3e2bd163c5a2b4cf34adade2c0e33"
    ];
    let combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, CHUCK_SECRET_PHRASE, "chuck, 3of5, version 0");

    pieces = [
        "1:9999:3:2:0:3:626cef7a2bfe67d3ecc2168c32e8a460db5c2c71adaed3b9",
        "1:9999:3:2:0:2:01d8ce9df0a2bbc29140a56211262d9449d501508b1c5547e5"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, ALICE_SECRET_PHRASE, "alice, 2of3, version 1");

    pieces = [
        "3:1539292261:3:2:0:2:00a1d4415680c2d34ee9bf1644f1f474138667995199828c15ebd6281108060a21",
        "3:1539292261:3:2:0:1:6d07741e869f03ccd4837d7c33984bc5abf149e6049a498fc8d5a70897ed5838"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, NRS.getPrivateKey(BOB_SECRET_PHRASE), "bob, 2of3, version 3");

    pieces = [
        "3:1904865159:5:3:0:2:00d5b5c5340ddd65f4de8fb69ddcd316963d994e6156bbf55f695f5afe22dfa4",
        "3:1904865159:5:3:0:3:01080e4754f642942f0f5213543454d1e26a2f1280b9c35499d372e5d1216da8",
        "3:1904865159:5:3:0:4:0149c465f7e9d5fa03dce350bc1fce696512f62c45595596683b7c986afe16ea"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, NRS.getPrivateKey(CHUCK_SECRET_PHRASE), "chuck, 3of5, version 3");
});

QUnit.test('combineFromServerSharesBIP39', function (assert) {
    let pieces = [
        "2:-2065004041:3:2:0:1:00a44541b246a3f8d47f2850fc64a2540963",
        "2:-2065004041:3:2:0:2:00c90b03e50dc872297ed1227949c52893ba",
        "2:-2065004041:3:2:0:3:00edd0c617d4eceb7e7e79f3f62ee7fd1e11"
    ];
    let combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'legal winner thank year wave sausage worth useful legal winner thank yellow', 'bip39 vector 2, 2of3, version 2');
    pieces = [
        "2:386848400:3:2:0:1:00ecdd92bc7c4b7ef575b5abe86ccf4113b3",
        "2:386848400:3:2:0:2:01593aa4f878167d6a6aead750591e01a75a",
        "2:386848400:3:2:0:3:01c597b73473e17bdf602002b8456cc23b01"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'letter advice cage absurd amount doctor acoustic avoid letter advice cage above', 'bip39 vector 3, 2of3, version 2');
    pieces = [
        "2:733275662:3:2:0:1:014983a3a95be80b0747759904931562852f",
        "2:733275662:3:2:0:2:0193074752b7d0160e8eeb3209262ac50b52",
        "2:733275662:3:2:0:3:01dc8aeafc13b82115d660cb0db940279175"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong', 'bip39 vector 4, 2of3, version 2');
    pieces = [
        "2:62298810:3:2:0:1:00bd6d0cd3546d20c6b4ef7cab74221694588d6da86cd33c435e",
        "2:62298810:3:2:0:2:00fb5a9a27295ac20dea5f79d768c4ada9319b5bd15a26f907aa",
        "2:62298810:3:2:0:3:013948277afe4863551fcf77035d6744be0aa949fa477ab5cbf6"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth useful legal will', 'bip39 vector 6, 2of3, version 2');
    pieces = [
        "2:-1343577490:3:2:0:1:00fbd2e40812aca8c9f4705c497a55a7ba862705f0f94fdf5fea",
        "2:-1343577490:3:2:0:2:017725478fa4d8d11368603812742acef48bcd8b61721f3e3fc2",
        "2:-1343577490:3:2:0:3:01f277ab173704f95cdc5013db6dfff62e917410d1eaee9d1f9a"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter always', 'bip39 vector 7, 2of3, version 2');
    pieces = [
        "2:1810790863:3:2:0:1:0165d3a0aef46958dee28ef0b0a3a687d4bf3e718a78402b10f6",
        "2:1810790863:3:2:0:2:01cba7415de8d2b1bdc51de161474d0fa97e7ce314f0805622da",
        "2:1810790863:3:2:0:3:02317ae20cdd3c0a9ca7acd211eaf3977e3dbb549f68c08134be"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo when', 'bip39 vector 8, 2of3, version 2');
    pieces = [
        "2:1872396651:3:2:0:1:00ef32a3ddd5e0ba7eba5c0f0d38d58c14db8e6cf417d0fc34d70731b0f1baa6cb35",
        "2:1872396651:3:2:0:2:015ee5c83c2c41f57df5389e9af22b98aa379d5a68b02278ea2e8ee3e263f5ce1752",
        "2:1872396651:3:2:0:3:01ce98ec9a82a3307d30152e28ab81a53f93ac47dd4873f59f86169613d630f5636f"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth useful legal winner thank year wave sausage worth title', 'bip39 vector 10, 2of3, version 2');
    pieces = [
        "2:970695243:3:2:0:1:00a08b252a1a952eb9dce5f5b27944c5967f1ea01864be080341d34179d49ca88445",
        "2:970695243:3:2:0:2:00c095c9d3b4a9dcf3394b6ae472090aac7dbcbfb048fb8f860326027328b8d08872",
        "2:970695243:3:2:0:3:00e0a06e7d4ebe8b2c95b0e0166acd4fc27c5adf482d391708c478c36c7cd4f88c9f"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic bless', 'bip39 vector 11, 2of3, version 2');
    pieces = [
        "2:-359908571:3:2:0:1:0189fade44c91ef7a6ab4bdcdcf42211c632c9ce96580c312f11a7557daf38150881",
        "2:-359908571:3:2:0:2:0213f5bc89923def4d5697b9b9e844238c65939d2cb018625e234eaafb5e702a11ea",
        "2:-359908571:3:2:0:3:029df09ace5b5ce6f401e39696dc663552985d6bc30824938d34f600790da83f1b53"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote', 'bip39 vector 12, 2of3, version 2');
    pieces = [
        "2:-1953591143:3:2:0:1:00b5b7a17f8b1221e95a56d5307338958d71",
        "2:-1953591143:3:2:0:2:00f3ac92f7ff557cb17c7494c2a623760dd6",
        "2:-1953591143:3:2:0:3:0131a184707398d7799e925454d90e568e3b"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'jelly better achieve collect unaware mountain thought cargo oxygen act hood bridge', 'bip39 vector 13, 2of3, version 2');
    pieces = [
        "2:1284742558:3:2:0:1:00c6cdf04364fc2029071b59b3a7f4b2c54067e54f95122245e3",
        "2:1284742558:3:2:0:2:00d761442d2311fe5f854af2644e6a47e088a6bf6183665510b4",
        "2:1284742558:3:2:0:3:00e7f49816e127dc96037a8b14f4dfdcfbd0e5997371ba87db85"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'renew stay biology evidence goat welcome casual join adapt armor shuffle fault little machine walk stumble urge swap', 'bip39 vector 14, 2of3, version 2');
    pieces = [
        "2:-607018676:3:2:0:1:7520a3779f29440d7da127edcc65cdf577670433ff5736c8d3cbe472ebaaf62b9c",
        "2:-607018676:3:2:0:2:00ac2d30e584d95464913c420dbc59a11915b7908c778e3ba9484bc78978d762d520",
        "2:-607018676:3:2:0:3:00e339be536a8964bba4d75c2dac4d743cb4081ce4efc54089bccbaaa00603cf7ea4"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'dignity pass list indicate nasty swamp pool script soccer toe leaf photo multiply desk host tomato cradle drill spread actor shine dismiss champion exotic', 'bip39 vector 15, 2of3, version 2');
    pieces = [
        "2:-1551570740:3:2:0:1:04b7d3ec3c65f05138668bae93b96467ee",
        "2:-1551570740:3:2:0:2:050eb8912075dbdd0ac6fe81f9084a50d0",
        "2:-1551570740:3:2:0:3:05659d360485c768dd2771555e573039b2"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'afford alter spike radar gate glance object seek swamp infant panel yellow', 'bip39 vector 16, 2of3, version 2');
    pieces = [
        "2:589570830:3:2:0:1:00843b8dd92f33988ff56818a9e98899fd8f54a7ea4db1076c23",
        "2:589570830:3:2:0:2:0095810cf79889a6425da60bac3c0107be3c8d8faad962076934",
        "2:589570830:3:2:0:3:00a6c68c1601dfb3f4c5e3feae8e79757ee9c6776b6513076645"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'indicate race push merry suffer human cruise dwarf pole review arch keep canvas theme poem divorce alter left', 'bip39 vector 17, 2of3, version 2');
    pieces = [
        "2:634075503:3:2:0:1:3abce90ea230c2618838a5d21c45ccbddfbebd34d85921bc2dbe90e88da6ae5890",
        "2:634075503:3:2:0:2:48f3e2555212a06bd345c9fd4c24ca99b5ca9daca6d9559c099d00760fe4789b08",
        "2:634075503:3:2:0:3:572adb9c01f47e761e52ee287c03c8758bd67e247559897be57b7003922242dd80"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'clutch control vehicle tonight unusual clog visa ice plunge glimpse recipe series open hour vintage deposit universe tip job dress radar refuse motion taste', 'bip39 vector 18, 2of3, version 2');
    pieces = [
        "2:505564807:3:2:0:1:0187009b6a797f5e4513a5310eebe56abe21",
        "2:505564807:3:2:0:2:0223158b22bacb6a8cf5735e99cc97eb9a36",
        "2:505564807:3:2:0:3:02bf2a7adafc1776d4d7418c24ad4a6c764b"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'turtle front uncle idea crush write shrug there lottery flower risk shell', 'bip39 vector 19, 2of3, version 2');
    pieces = [
        "2:20339901:3:2:0:1:009619086438011ec2ac97f9ac41505ebb334445484403cf9b76",
        "2:20339901:3:2:0:2:00b16db3c9f8df4f18dd87a39c5544e75b20bd5aaa9ca1f4beda",
        "2:20339901:3:2:0:3:00ccc25f2fb9bd7f6f0e774d8c69396ffb0e36700cf54019e23e"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'kiss carry display unusual confirm curtain upgrade antique rotate hello void custom frequent obey nut hole price segment', 'bip39 vector 20, 2of3, version 2');
    pieces = [
        "2:1004342761:3:2:0:1:74d86b21964a40228ac25a5c1aca68d5f97e89431a06ed3457e183700dfb6bc794",
        "2:1004342761:3:2:0:2:009a0f2d86ee26ff57026eafa9af68b999efe87f65088f1674f4a7fdeeb32c18a010",
        "2:1004342761:3:2:0:3:00bf45efec4603be8b7a1b04f744070a5de6527586f7173fb5916e786d585cc5788c"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'exile ask congress lamp submit jacket era scheme attend cousin alcohol catch course end lucky hurt sentence oven short ball bird grab wing top', 'bip39 vector 21, 2of3, version 2');
    pieces = [
        "2:-1571722963:3:2:0:1:3035ab49cc6ae2fe3e74b62488265adbb4",
        "2:-1571722963:3:2:0:2:47c03ce9a38b33878cab1a3f6da02b265c",
        "2:-1571722963:3:2:0:3:5f4ace897aab8410dae17e5a5319fb7104"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'board flee heavy tunnel powder denial science ski answer betray cargo cat', 'bip39 vector 22, 2of3, version 2');
    pieces = [
        "2:-1836081629:3:2:0:1:310bca34ebf45e252e6b4a0d62647f0f23f240a5a1d0c73db5",
        "2:-1836081629:3:2:0:2:4974b291bc59ec97b9a2e64fb8b158645c6db4ee3dc5fcd758",
        "2:-1836081629:3:2:0:3:61dd9aee8cbf7b0a44da82920efe31b994e92936d9bb3270fb"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'board blade invite damage undo sun mimic interest slam gaze truly inherit resist great inject rocket museum chief', 'bip39 vector 23, 2of3, version 2');
    pieces = [
        "2:-576656713:3:2:0:1:2a811aef4e6e2c09fe66cfa9296169c9686c331909f8334c3c540e3c27336dd451",
        "2:-576656713:3:2:0:2:3f27aeb2073b1a3cc3d1aa44104d4fe56ee6d658748dda56fe464cf50a9dd78f8a",
        "2:-576656713:3:2:0:3:53ce4274c008086f893c84def739360175617997df238161c0388badee08414ac3"
    ];
    combinedSecret = sss.combineSecret(pieces);
    assert.equal(combinedSecret, 'beyond stage sleep clip because twist token leaf atom beauty genius food business side grid unable middle armed observe pair crouch tonight away coconut', 'bip39 vector 24, 2of3, version 2');
});
