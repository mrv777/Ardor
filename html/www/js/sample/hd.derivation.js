/******************************************************************************
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

let loader = require("./loader");

loader.load(async function(NRS) {
    const BASE_PATH = "m/44'/16754'";
    const MAX_INT = Math.pow(2, 31);
    for (let i=0; i<256; i++) {
        for (let numberOfWords = 12; numberOfWords <= 24; numberOfWords += 3) {
            // Generate random mnemonic and its random passphrase
            let mnemonic = BIP39.generateMnemonic((numberOfWords * 11 * 32) / 33, NRS.constants.SECRET_WORDS);
            let passphrase = converters.byteArrayToHexString(crypto.randomBytes(32));

            // Generate a random bip32 path with a mix of hard and soft derivation
            let depth = Math.floor(Math.random() * 7) + 1;
            let path = BASE_PATH;
            for (let j = 0; j < depth; j++) {
                path += "/" + Math.floor(Math.random() * MAX_INT);
                if (Math.floor(Math.random() * 2) === 1) {
                    path += "'";
                }
            }
            NRS.logConsole(i + ": " + path + ": " + mnemonic + ", " + passphrase);

            // Derive a bip32 node using the server API
            let derivedAccountResponse = await NRS.sendRequestAndWait("deriveAccountFromSeed", {
                mnemonic: mnemonic,
                passphrase: passphrase,
                bip32Path: path
            });

            // Derive a bip32 node using client API
            let seed = KeyDerivation.mnemonicAndPassphraseToSeed(mnemonic, passphrase);
            let clientBip32Node = KeyDerivation.deriveSeed(path, seed);

            // Compare the derivation data to make sure it is identical
            if (converters.byteArrayToHexString(clientBip32Node.getPrivateKeyLeft()) !== derivedAccountResponse.privateKey) {
                throw new Error();
            }
            if (converters.byteArrayToHexString(clientBip32Node.getPrivateKeyRight()) !== derivedAccountResponse.privateKeyRight) {
                throw new Error();
            }
            if (converters.byteArrayToHexString(clientBip32Node.getSerializedMasterPublicKey()) !== derivedAccountResponse.serializedMasterPublicKey) {
                throw new Error();
            }
            if (converters.byteArrayToHexString(clientBip32Node.getPublicKey()) !== derivedAccountResponse.publicKey) {
                throw new Error();
            }

            // Derive a random child using public key derivation both server side and client side and compare the resulting public key
            for (let j=0; j<256; j++) {
                let childIndex = Math.floor(Math.random() * Math.pow(2, 16));
                let deriveChildPublicDataResponse = await NRS.sendRequestAndWait("deriveAccountFromMasterPublicKey", {
                    serializedMasterPublicKey: derivedAccountResponse.serializedMasterPublicKey,
                    childIndex: childIndex
                });
                let clientPublicChildBip32Node = KeyDerivation.deriveChildPublicKeyFromNode(clientBip32Node, childIndex);
                if (converters.byteArrayToHexString(clientPublicChildBip32Node.getPublicKey()) !== deriveChildPublicDataResponse.publicKey) {
                    throw new Error(`e1 i=${i}, numberOfWords=${numberOfWords}, j=${j}`);
                }
                let deriveChildPrivateDataResponse = await NRS.sendRequestAndWait("deriveAccountFromSeed", {
                    mnemonic: mnemonic,
                    passphrase: passphrase,
                    bip32Path: path + "/" + childIndex
                });
                if (deriveChildPrivateDataResponse.publicKey !== deriveChildPublicDataResponse.publicKey) {
                    throw new Error(`e2 i=${i}, numberOfWords=${numberOfWords}, j=${j}`);
                }
                let clientPrivateChildBip32Node = KeyDerivation.deriveChildPrivateKey(clientBip32Node, childIndex);
                if (converters.byteArrayToHexString(clientPrivateChildBip32Node.getPublicKey()) !== deriveChildPublicDataResponse.publicKey) {
                    throw new Error(`e3 i=${i}, numberOfWords=${numberOfWords}, j=${j}`);
                }
            }
        }
    }
});
