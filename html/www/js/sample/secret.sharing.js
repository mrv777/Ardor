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
    for (let i=0; i<256; i++) {
        for (let numberOfWords = 12; numberOfWords <= 24; numberOfWords += 3) {
            let mnemonic = BIP39.generateMnemonic((numberOfWords * 11 * 32) / 33, NRS.constants.SECRET_WORDS);
            NRS.logConsole(mnemonic);

            // Split and combine secret client side
            let clientPieces = sss.splitPhrase(mnemonic, 3, 2);
            let combine12 = sss.combineSecret([clientPieces[0], clientPieces[1]]);
            if (combine12 !== mnemonic) {
                throw new Error();
            }
            let combine13 = sss.combineSecret([clientPieces[0], clientPieces[2]]);
            if (combine13 !== mnemonic) {
                throw new Error();
            }
            let combine23 = sss.combineSecret([clientPieces[1], clientPieces[2]]);
            if (combine23 !== mnemonic) {
                throw new Error();
            }
            let combine123 = sss.combineSecret([clientPieces[0], clientPieces[1], clientPieces[2]]);
            if (combine123 !== mnemonic) {
                throw new Error();
            }
            try {
                sss.combineSecret([clientPieces[0], clientPieces[0]]);
                NRS.logConsole("fail");
            } catch (e) {}

            // Split secret server side combine secret client side
            let splitResponse = await NRS.sendRequestAndWait("splitSecret", { secret: mnemonic, totalPieces: 3, minimumPieces: 2 });
            if (splitResponse.errorCode) {
                throw new Error("split request failed");
            }
            let serverPieces = splitResponse.pieces;
            combine12 = sss.combineSecret([serverPieces[0], serverPieces[1]]);
            if (combine12 !== mnemonic) {
                throw new Error();
            }
            combine13 = sss.combineSecret([serverPieces[0], serverPieces[2]]);
            if (combine13 !== mnemonic) {
                throw new Error();
            }
            combine23 = sss.combineSecret([serverPieces[1], serverPieces[2]]);
            if (combine23 !== mnemonic) {
                throw new Error();
            }

            // Split secret client side combine secret server side
            let combineResponse = await NRS.sendRequestAndWait("combineSecret", { pieces: [clientPieces[0], clientPieces[1]] });
            let serverCombine = combineResponse.secret;
            if (serverCombine !== mnemonic) {
                throw new Error();
            }
            combineResponse = await NRS.sendRequestAndWait("combineSecret", { pieces: [clientPieces[0], clientPieces[2]] });
            serverCombine = combineResponse.secret;
            if (serverCombine !== mnemonic) {
                throw new Error();
            }
            combineResponse = await NRS.sendRequestAndWait("combineSecret", { pieces: [clientPieces[1], clientPieces[2]] });
            serverCombine = combineResponse.secret;
            if (serverCombine !== mnemonic) {
                throw new Error();
            }

            let derivedAccount = await NRS.sendRequestAndWait("deriveAccountFromSeed", { mnemonic: mnemonic, bip32Path: "m/44'/16754'/0'/0'/0" });
            let privateKey = derivedAccount.privateKey;

            // Split private key and combine client side
            clientPieces = sss.splitPrivateKey(privateKey, 3, 2);
            combine12 = sss.combineSecret([clientPieces[0], clientPieces[1]]);
            if (combine12 !== privateKey) {
                throw new Error();
            }
            combine13 = sss.combineSecret([clientPieces[0], clientPieces[2]]);
            if (combine13 !== privateKey) {
                throw new Error();
            }
            combine23 = sss.combineSecret([clientPieces[1], clientPieces[2]]);
            if (combine23 !== privateKey) {
                throw new Error();
            }
            combine123 = sss.combineSecret([clientPieces[0], clientPieces[1], clientPieces[2]]);
            if (combine123 !== privateKey) {
                throw new Error();
            }
            try {
                sss.combineSecret([clientPieces[0], clientPieces[0]]);
                NRS.logConsole("fail");
            } catch (e) {}

            // Split private key server side combine private key client side
            splitResponse = await NRS.sendRequestAndWait("splitSecret", { privateKey: privateKey, totalPieces: 3, minimumPieces: 2 });
            if (splitResponse.errorCode) {
                throw new Error("split request failed");
            }
            serverPieces = splitResponse.pieces;
            combine12 = sss.combineSecret([serverPieces[0], serverPieces[1]]);
            if (combine12 !== privateKey) {
                throw new Error();
            }
            combine13 = sss.combineSecret([serverPieces[0], serverPieces[2]]);
            if (combine13 !== privateKey) {
                throw new Error();
            }
            combine23 = sss.combineSecret([serverPieces[1], serverPieces[2]]);
            if (combine23 !== privateKey) {
                throw new Error();
            }

            // Combine private key server side
            combineResponse = await NRS.sendRequestAndWait("combineSecret", { pieces: [clientPieces[0], clientPieces[1]] });
            serverCombine = combineResponse.privateKey;
            if (serverCombine !== privateKey) {
                throw new Error();
            }
            combineResponse = await NRS.sendRequestAndWait("combineSecret", { pieces: [clientPieces[0], clientPieces[2]] });
            serverCombine = combineResponse.privateKey;
            if (serverCombine !== privateKey) {
                throw new Error();
            }
            combineResponse = await NRS.sendRequestAndWait("combineSecret", { pieces: [clientPieces[1], clientPieces[2]] });
            serverCombine = combineResponse.privateKey;
            if (serverCombine !== privateKey) {
                throw new Error();
            }
        }
    }
});
