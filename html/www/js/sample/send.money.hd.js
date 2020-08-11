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

var loader = require("./loader");
var config = loader.config;

loader.load(function(NRS) {
    let data = {
        amountNQT: NRS.convertToNQT("0.01"), // NXT to NQT conversion
        chain: config.chain,
    };

    data = Object.assign(
        data,
        NRS.getMandatoryParams(),
    );

    const seed = "opinion change copy struggle town cigar input kit school patient execute bird bundle option canvas defense hover poverty skill donkey pottery infant sense orchard";

    // Derive sender private key from seed, bip32 path on testnet m/44'/16754'/0'/1'/1
    let senderPath = NRS.constants.BIP32_PATH_PREFIX.slice();
    senderPath.push(1); // Sender child index
    let senderNode = KeyDerivation.deriveMnemonic(BIPPath.fromPathArray(senderPath).toString(), seed);
    data.privateKey = converters.byteArrayToHexString(senderNode .getPrivateKeyLeft());

    // Derive recipient public key and address from seed, bip32 path on testnet m/44'/16754'/0'/1'/0
    let recipientPath = NRS.constants.BIP32_PATH_PREFIX.slice();
    recipientPath.push(0); // Recipient child index
    let recipientNode = KeyDerivation.deriveMnemonic(BIPPath.fromPathArray(recipientPath).toString(), seed);
    let publicKey = converters.byteArrayToHexString(recipientNode.getPublicKey());
    data.recipientPublicKey = publicKey;
    data.recipient = NRS.getAccountIdFromPublicKey(publicKey);

    // Submit the request to the remote node using the standard client function which performs local signing for transactions
    // and validates the data returned from the server.
    // This method will only send the passphrase to the server in requests for which the passphrase is required like startForging
    // It will never submit the passphrase for transaction requests
    NRS.sendRequest("sendMoney", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
