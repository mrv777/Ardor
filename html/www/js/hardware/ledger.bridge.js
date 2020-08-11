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

/**
 * @depends {nrs.js}
 * The methods below provide a bridge between the wallet UI and the hardware wallet. The code supports the ledger
 * interface using the hardwareWallet object.
 * When running the browser wallet, hardwareWallet represents the bridge to the ledger Javascript library which
 * uses WebUSB for ledger communication.
 * When running the desktop wallet, hardwareWallet invokes the Java based ledger interface which is based on HID.
 * Since the Java interface can only receive string and primitive parameters and return byte array, string or
 * primitives, you see some seemingly obsolete conversion of byte to hex string and vice versa but these are necessary.
 */
var NRS = (function (NRS) {
    const isDesktopApplication = navigator.userAgent.indexOf("JavaFX") >= 0;
    let currentState = "";
    let hardwareWallet;

    function getHardwareWallet() {
        if (isDesktopApplication) {
            hardwareWallet = new JavaHardwareWallet();
        } else {
            if (hardwareWallet !== undefined) {
                return hardwareWallet;
            }
            hardwareWallet = new JsHardwareWallet();
            hardwareWallet.registerCallback(hardwareListener);
        }
        return hardwareWallet;
    }

    /**
     * Only supported by Javascript at the moment not by the desktop wallet
     * @param state the new state
     */
    function hardwareListener(state) {
        const hardwareWallet = getHardwareWallet();
        NRS.logConsole("hardware state changed from " + currentState + " to " + state);
        hardwareWallet.setLastError(state);
    }

    function timeout(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    NRS.getPublicKeyFromHardwareWallet = async function(pathStr, isGetMasterKey, $callout) {
        let hardwareWallet = getHardwareWallet();
        let getPublicKeysResponse = await hardwareWallet.getWalletPublicKeys(pathStr, isGetMasterKey);
        NRS.logConsole("ledger getPublicKeys response: " + converters.byteArrayToHexString(getPublicKeysResponse));
        if (getPublicKeysResponse === undefined || !isGetMasterKey && getPublicKeysResponse.length !== 32 || isGetMasterKey && getPublicKeysResponse.length !== 3*32) {
            NRS.logConsole("get public key failed for " + pathStr + ", " + hardwareWallet.getLastError());
            if ($callout && hardwareWallet.getLastError()) {
                $callout.text($.t(hardwareWallet.getLastError().toLowerCase()));
            }
            return [];
        }
        return getPublicKeysResponse;
    };

    NRS.signTokenOnHardwareWallet = async function(timestamp, messageHex) {
        const $hardwareStatus = $(".hardware-status");
        $hardwareStatus.html($.t("signing_token_on_hardware_wallet"));
        let token = await getHardwareWallet().signToken(NRS.bip32Account.getPath(), timestamp, messageHex);
        $hardwareStatus.html($.t("hardware_wallet_waiting_for_commands"));
        return token;
    };

    NRS.showAddressOnHardwareWallet = async function(pathStr) {
        await getHardwareWallet().showAddress(pathStr);
    };

    NRS.signBytesUsingHardwareWallet = async function(hex) {
        let bytes = converters.hexStringToByteArray(hex);
        const $hardwareStatus = $(".hardware-status");
        $hardwareStatus.html($.t("loading_transaction_to_hardware_wallet"));
        let hardwareWallet = getHardwareWallet();
        const loadTxnResponse = await hardwareWallet.loadWalletTransaction(converters.byteArrayToHexString(bytes));
        if (loadTxnResponse === true) {
            $hardwareStatus.html($.t("signing_transaction_on_hardware_wallet"));
            const signTxnResponse = await hardwareWallet.signWalletTransaction(NRS.bip32Account.getPath());
            if (signTxnResponse.length === 64) {
                $hardwareStatus.html($.t("hardware_wallet_waiting_for_commands"));
                return converters.byteArrayToHexString(signTxnResponse);
            } else {
                let error = hardwareWallet.getLastError();
                NRS.logConsole(error);
                $hardwareStatus.html($.t("hardware_wallet_sign_txn_error", {rc: error}));
                $.growl($.t("hardware_wallet_sign_txn_error", {rc: error}), { "type": "danger", "offset": 10 });
                return null;
            }
        } else {
            let error = hardwareWallet.getLastError();
            NRS.logConsole(error);
            $hardwareStatus.html($.t("hardware_wallet_load_txn_error", {rc: error}));
            $.growl($.t("hardware_wallet_load_txn_error", {rc: error}), { "type": "danger", "offset": 10 });
            return null;
        }
    };

    NRS.encryptUsingHardwareWallet = async function(publicKey, message) {
        const $hardwareStatus = $(".hardware-status");
        $hardwareStatus.html($.t("encrypting_message_on_hardware_wallet"));
        let hexMessage = typeof(message) === "string" ? converters.stringToHexString(message) : converters.byteArrayToHexString(message);
        let ret = await getHardwareWallet().encryptBuffer(NRS.bip32Account.getPath(), publicKey, hexMessage);
        if (ret === null) {
            $hardwareStatus.html($.t("encryption_error"));
            throw { message: getHardwareWallet().getLastError() + " encrypting message " + message + " with public key " + publicKey + " and path " + NRS.bip32Account.getPath() };
        }
        $hardwareStatus.html($.t("hardware_wallet_waiting_for_commands"));
        let tokens = ret.split(",");
        return { message: tokens[0], nonce: tokens[1] };
    };

    NRS.decryptUsingHardwareWallet = async function(data, options) {
        const $hardwareStatus = $(".hardware-status");
        $hardwareStatus.html($.t("decrypting_message_on_hardware_wallet"));
        let ret = await getHardwareWallet().decryptBuffer(NRS.bip32Account.getPath(), options.publicKey, options.nonce, data);
        if (ret === null) {
            throw { message: getHardwareWallet().getLastError() + " decrypting data " + data + " with nonce " + options.nonce + " public key " + options.publicKey + " and path " + NRS.bip32Account.getPath() };
        }
        let tokens = ret.split(",");
        $hardwareStatus.html($.t("hardware_wallet_waiting_for_commands"));
        let message;
        if (!(options.isText === false)) {
            message = converters.hexStringToString(tokens[0]);
        } else {
            message = tokens[0];
        }
        return { message: message, sharedKey: tokens[1] };
    };

    const JavaHardwareWallet = function () {
        let javaWallet = window.java.getHardwareWallet();
        return {
            getWalletPublicKeys: async function(pathStr, isGetMasterKey) {
                await timeout(50);
                return javaWallet.getWalletPublicKeys(pathStr, isGetMasterKey);
            },
            signToken: async function(pathStr, timestamp, messageHex) {
                await timeout(50);
                return javaWallet.signToken(pathStr, timestamp, messageHex);
            },
            loadWalletTransaction: async function(hex) {
                await timeout(50);
                return javaWallet.loadWalletTransaction(hex);
            },
            signWalletTransaction: async function(pathStr) {
                await timeout(50);
                return javaWallet.signWalletTransaction(pathStr);
            },
            encryptBuffer: async function(pathStr, publicKey, message) {
                await timeout(50);
                return javaWallet.encryptBuffer(pathStr, publicKey, message);
            },
            decryptBuffer: async function(pathStr, publicKey, nonce, encryptedData) {
                await timeout(50);
                return javaWallet.decryptBuffer(pathStr, publicKey, nonce, encryptedData);
            },
            showAddress: async function(pathStr) {
                await timeout(50);
                return javaWallet.showAddress(pathStr);
            },
            getLastError: function() {
                return javaWallet.getLastError();
            },

        };
    };

    const JsHardwareWallet = function () {
        return {
            checkConnection: async function() {
                const connectResponse = await ledgerwallet.connect();
                if (connectResponse === null) {
                    NRS.logConsole(this.getLastError());
                    return false;
                }
                return true;
            },
            getWalletPublicKeys: async function (pathStr, isGetMasterKey) {
                if (!await this.checkConnection()) {
                    return [];
                }
                const path = BIPPath.fromString(pathStr).toPathArray();
                const getPublicKeysResponse = await ledgerwallet.getPublicKeyAndChainCode(path, isGetMasterKey);
                NRS.logConsole("ledger getPublicKeys response: " + JSON.stringify(getPublicKeysResponse));
                if (getPublicKeysResponse.rc !== 0) {
                    if (getPublicKeysResponse.rc === 11) {
                        currentState = "device_locked";
                    } else {
                        currentState = "Error Code " + getPublicKeysResponse.rc;
                    }
                    return [];
                }
                if (isGetMasterKey) {
                    let data = new Uint8Array(3*32);
                    data.set(getPublicKeysResponse.data.publicKeyCurve);
                    data.set(getPublicKeysResponse.data.publicKeyEd25519, 32);
                    data.set(getPublicKeysResponse.data.chainCode, 64);
                    return Array.from(data)
                } else {
                    return Array.from(getPublicKeysResponse.data.publicKeyCurve);
                }
            },
            signToken: async function (pathStr, timestamp, messageHex) {
                if (!await this.checkConnection()) {
                    return "";
                }
                const path = BIPPath.fromString(pathStr).toPathArray();
                const signTokenResponse = await ledgerwallet.signToken(path, timestamp, converters.hexStringToByteArray(messageHex));
                if (signTokenResponse.rc === 0) {
                    return signTokenResponse.data;
                } else {
                    currentState = $.t("hardware_wallet_sign_txn_error", {rc: signTokenResponse.rc});
                    NRS.logConsole(currentState);
                    return "";
                }
            },
            loadWalletTransaction: async function(hex) {
                if (!await this.checkConnection()) {
                    return false;
                }
                const loadTxnResponse = await ledgerwallet.loadTxn(converters.hexStringToByteArray(hex));
                if (loadTxnResponse.data === true) {
                    return true;
                } else {
                    if (loadTxnResponse.rc === 0) {
                        currentState = $.t("hardware_wallet_txn_signing_cancelled");
                    } else {
                        currentState = $.t("hardware_wallet_load_txn_error", {rc: loadTxnResponse.rc});
                    }
                    NRS.logConsole(currentState);
                    return false;
                }
            },
            signWalletTransaction: async function (pathStr) {
                if (!await this.checkConnection()) {
                    return [];
                }
                const path = BIPPath.fromString(pathStr).toPathArray();
                const signTxnResponse = await ledgerwallet.signTxn(path);
                if (signTxnResponse.rc === 0) {
                    return signTxnResponse.data;
                } else {
                    currentState = $.t("hardware_wallet_sign_txn_error", {rc: signTxnResponse.rc});
                    NRS.logConsole(currentState);
                    return [];
                }
            },
            encryptBuffer: async function(pathStr, publicKey, message) {
                if (!await this.checkConnection()) {
                    return null;
                }
                const path = BIPPath.fromString(pathStr).toPathArray();
                let messageBytes = converters.hexStringToByteArray(message);
                let compressedMessageBytes = pako.gzip(new Uint8Array(messageBytes));
                let paddedBytes = NRS.pkcs7Pad(compressedMessageBytes);
                const encryptResponse = await ledgerwallet.encryptBuffer(path, converters.hexStringToByteArray(publicKey), paddedBytes);
                if (encryptResponse.rc === 0) {
                    return converters.byteArrayToHexString(encryptResponse.data.IV) +
                        converters.byteArrayToHexString(encryptResponse.data.encryptedData) + "," + converters.byteArrayToHexString(encryptResponse.data.nonce);
                } else {
                    currentState = $.t("hardware_wallet_encrypt_buffer_error", {rc: encryptResponse.rc });
                    NRS.logConsole(currentState);
                    return null;
                }
            },
            decryptBuffer: async function(pathStr, publicKey, nonce, encryptedData) {
                if (!await this.checkConnection()) {
                    return null;
                }
                let iv = encryptedData.substring(0, 32);
                encryptedData = encryptedData.substring(32);
                const path = BIPPath.fromString(pathStr).toPathArray();
                const decryptResponse = await ledgerwallet.decryptBuffer(path,
                    converters.hexStringToByteArray(publicKey),
                    converters.hexStringToByteArray(nonce),
                    converters.hexStringToByteArray(iv),
                    converters.hexStringToByteArray(encryptedData),
                    true);
                if (decryptResponse.rc === 0) {
                    let result = NRS.pkcs7Unpad(decryptResponse.data.resultBuffer);
                    let uncompressedResult = pako.inflate(result);
                    return converters.byteArrayToHexString(uncompressedResult) + "," + converters.byteArrayToHexString(decryptResponse.data.sharedKey);
                } else {
                    currentState = $.t("hardware_wallet_decrypt_buffer_error", {rc: decryptResponse.rc });
                    NRS.logConsole(currentState);
                    return null;
                }
            },
            showAddress: async function(pathStr) {
                if (!await this.checkConnection()) {
                    return;
                }
                const path = BIPPath.fromString(pathStr).toPathArray();
                await ledgerwallet.showAddress(path);
            },
            getLastError: function () {
                return currentState;
            },
            setLastError: function (state) {
                currentState = state;
            },
            registerCallback: function (callback) {
                ledgerwallet.registerCallBack(callback);
            }
        };
    };

    return NRS;
}(isNode ? client : NRS || {}, jQuery));