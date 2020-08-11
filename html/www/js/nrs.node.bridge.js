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

const options = {
    url: "http://localhost:27876", // URL of remote node
    disableSslCertificateVerification: false, // Set to true to allow self signed SSL certificate when using https
    secretPhrase: "", // Secret phrase of the current account
    isTestNet: false, // Select testnet or mainnet
    chain: "2", // Defaults to IGNIS chain
    adminPassword: "" // Node admin password
};

exports.init = function(params) {
    if (!params) {
        return this;
    }
    options.url = params.url;
    options.disableSslCertificateVerification = params.disableSslCertificateVerification;
    options.secretPhrase = params.secretPhrase;
    options.isTestNet = params.isTestNet;
    options.chain = params.chain;
    options.adminPassword = params.adminPassword;
    return this;
};

/**
 * Load the necessary node modules and assign them to the global scope
 * the NXT client wasn't designed with modularity in mind therefore we need
 * to include every 3rd party library function in the global scope
 * @param callback function to invoke after loading the wallet resources
 */
exports.load = function(callback) {
    try {
        global.jQuery = function() {
            // Ignore all $() selectors
            return [];
        };
        global.jQuery.growl = function(msg) { console.log("growl: " + msg)}; // disable growl messages
        global.jQuery.t = function(text) { return text; }; // Disable the translation functionality
        global.$ = global.jQuery; // Needed by extensions.js
        // noinspection NodeCoreCodingAssistance
        global.crypto = require("crypto");
        global.fetch = require("node-fetch");
        global.CryptoJS = require("crypto-js");
        global.async = require("async");
        global.pako = require("pako");
        let jsbn = require('jsbn');
        global.BigInteger = jsbn.BigInteger;
        global.Big = require('big.js');
        global.BIPPath = require('bip32-path');
        global.bigInt = require('big-integer');
        global.CRC32 = require('crc-32');

        // Mock other objects on which the client depends
        global.document = {};
        global.isNode = true;

        global.navigator = {};
        global.navigator.userAgent = "";

        // Now load some specific product libraries into the global scope
        global.NxtAddress = require('./util/nxtaddress');
        global.curve25519 = require('./crypto/curve25519');
        global.curve25519_ = require('./crypto/curve25519_');
        global.KeyDerivation = require('./crypto/key.derivation');
        global.CurveConversion = require('./crypto/curve.conversion');
        global.Ed25519 = require('./crypto/ed25519');
        global.BIP39 = require('./crypto/bip39');
        global.sss = require('./crypto/sss.js');

        require('./util/extensions');
        global.converters = require('./util/converters');

        // Now start loading the client itself
        // The main challenge is that in node every JavaScript file is a module with it's own scope
        // however the client relies on a global browser scope which defines the NRS object
        // The idea here is to gradually compose the NRS object by adding functions from each
        // JavaScript file into the existing global.client scope
        global.client = {};
        global.client.isTestNet = options.isTestNet;
        global.client.deviceSettings = {};
        global.client.deviceSettings.chain = options.chain;
        global.client.onSiteBuildDone = function() {
            return Promise.resolve(true);
        };
        global.client = Object.assign(client, require('./nrs.encryption'));
        global.client = Object.assign(client, require('./nrs.feature.detection'));
        global.client = Object.assign(client, require('./nrs.transactions.types'));
        global.client = Object.assign(client, require('./nrs.constants'));
        global.client = Object.assign(client, require('./nrs.console'));
        global.client = Object.assign(client, require('./nrs.util'));
        global.client = Object.assign(client, require('./nrs.numeric'));
        global.client = Object.assign(client, require('./util/locale'));

        global.client.useHardwareWallet = function() {
            // TODO can we interface with a hardware wallet from Node JS?
            return false;
        };

        global.client.getModuleConfig = function() {
            return options;
        };
        global.client = Object.assign(client, require('./nrs.server'));

        // Now load the constants locally since we cannot trust the remote node to return the correct constants
        let constants;
        if (options.isTestNet) {
            constants = require('./data/constants.testnet');
        } else {
            constants = require('./data/constants.mainnet');
        }
        if (options.disableSslCertificateVerification) {
            process.env.NODE_TLS_REJECT_UNAUTHORIZED = 0;
        }
        global.client.onSiteBuildDone().then(() => {
            global.client.processConstants(constants);
            setCurrentAccount(options.secretPhrase); // todo can we use private key instead?
            callback(global.client);
        });
    } catch (e) {
        console.log(e.message);
        if (e.stack) {
            console.log(e.stack);
        }
        throw e;
    }
};

function setCurrentAccount(secretPhrase) {
    let privateKey = client.getPrivateKey(secretPhrase);
    client.account = client.getAccountId(privateKey);
    client.accountRS = client.convertNumericToRSAccountFormat(client.account);
    client.accountInfo = {}; // Do not cache the public key
    client.resetEncryptionState();
}
exports.setCurrentAccount = setCurrentAccount;