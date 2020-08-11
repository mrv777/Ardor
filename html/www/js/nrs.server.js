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
 */
(isNode ? client : NRS).onSiteBuildDone().then(() => {
    NRS = (function (NRS, $, undefined) {

        const NXT_FIELDS = [
            ["feeNXT", "feeNQT"],
            ["amountNXT", "amountNQT"],
            ["priceNXT", "priceNQT"],
            ["refundNXT", "refundNQT"],
            ["discountNXT", "discountNQT"],
            ["phasingQuorumNXT", "phasingQuorum"],
            ["phasingMinBalanceNXT", "phasingMinBalance"],
            ["controlQuorumNXT", "controlQuorum"],
            ["controlMinBalanceNXT", "controlMinBalance"],
            ["controlMaxFeesNXT", "controlMaxFees"],
            ["minBalanceNXT", "minBalance"],
            ["shufflingAmountNXT", "amount"],
            ["standbyShufflerMinAmountNXT", "minAmount"],
            ["standbyShufflerMaxAmountNXT", "maxAmount"],
            ["monitorAmountNXT", "amount"],
            ["monitorThresholdNXT", "threshold"],
            ["minRateNXTPerFXT", "minRateNQTPerFXT"]
        ];

        const QNT_FIELDS = [
            ["phasingQuorumQNTf", "phasingHoldingDecimals"],
            ["phasingMinBalanceQNTf", "phasingHoldingDecimals"],
            ["controlQuorumQNTf", "controlHoldingDecimals"],
            ["controlMinBalanceQNTf", "controlHoldingDecimals"],
            ["minBalanceQNTf", "create_poll_asset_decimals"],
            ["minBalanceQNTf", "create_poll_ms_decimals"],
            ["amountQNTf", "shuffling_asset_decimals"],
            ["amountQNTf", "shuffling_ms_decimals"],
            ["amountQNTf", "funding_monitor_asset_decimals"],
            ["amountQNTf", "funding_monitor_ms_decimals"],
            ["thresholdQNTf", "funding_monitor_asset_decimals"],
            ["thresholdQNTf", "funding_monitor_ms_decimals"],
            ["minAmountQNTf", "standbyshuffler_asset_decimals"],
            ["minAmountQNTf", "standbyshuffler_ms_decimals"],
            ["maxAmountQNTf", "standbyshuffler_asset_decimals"],
            ["maxAmountQNTf", "standbyshuffler_ms_decimals"]
        ];

        const FEE_FIELDS = ["controlMaxFees"];

        let sessionPrivateKey;
        NRS.requestId = 0;

        NRS.setServerPrivateKey = function (privateKey) {
            sessionPrivateKey = privateKey;
        };

        function isVolatileRequest(data, requestType) {
            return (NRS.isPassphraseAtRisk() || data.doNotSign || data.isVoucher || NRS.isPrivateKeyStoredOnHardware()) && !NRS.isSubmitPassphrase(requestType);
        }

        function isResponseReadyForSigning(response, data, requestType) {
            return response.unsignedTransactionBytes && !data.doNotSign &&
                !response.errorCode && !response.error &&
                !response.bundlerRateNQTPerFXT && !data.calculateFee && requestType !== "getTransactionBytes";
        }

        function performDataConversion(data) {
            let field = "N/A";
            try {
                // convert coin to NQT
                for (let i = 0; i < NXT_FIELDS.length; i++) {
                    field = NXT_FIELDS[i][0];
                    let nqtField = NXT_FIELDS[i][1];
                    if (field in data) {
                        data[nqtField] = NRS.convertToNQT(data[field]);
                        delete data[field];
                    }
                }

                // convert asset/currency decimal amount to base unit
                let toDelete = [];
                for (let i = 0; i < QNT_FIELDS.length; i++) {
                    let decimalUnitField = QNT_FIELDS[i][0];
                    let decimalsField = QNT_FIELDS[i][1];
                    field = decimalUnitField.replace("QNTf", "");
                    if (decimalUnitField in data && decimalsField in data) {
                        let unitField = data[decimalUnitField];
                        if (!unitField) {
                            continue;
                        }
                        data[field] = NRS.convertToQNT(parseFloat(unitField), parseInt(data[decimalsField]));
                        toDelete.push(decimalUnitField);
                        toDelete.push(decimalsField);
                    }
                }
                for (let i = 0; i < toDelete.length; i++) {
                    delete data[toDelete[i]];
                }

                // Add chain id to fee field
                for (let i = 0; i < FEE_FIELDS.length; i++) {
                    field = FEE_FIELDS[i];
                    if (!data[field]) {
                        continue;
                    }
                    if (data[field] == "0") {
                        delete data[field];
                    } else {
                        data[field] = NRS.getActiveChainId() + ":" + data[field];
                    }
                }
                return {};
            } catch (err) {
                NRS.logException(err);
                return {
                    "errorCode": 1,
                    "errorDescription": "data conversion error " + err + " for field " + field
                }
            }
        }

        NRS.sendRequestAndWait = async function (requestType, data, callback, options) {
            return new Promise(function (resolve) {
                NRS.sendRequest(requestType, data, resolve, options);
            }).then(function (response) {
                response.requestData = data;
                return response;
            })
        };

        NRS.sendRequest = function (requestType, data, callback, options) {
            let msg;
            if (!options) {
                options = {};
            }
            if (requestType === undefined) {
                msg = "Undefined request type";
                NRS.logConsole(msg);
                callback({
                    "errorCode": 1,
                    "errorDescription": msg
                });
                return;
            }
            if (!NRS.isRequestTypeEnabled(requestType)) {
                callback({
                    "errorCode": 1,
                    "errorDescription": $.t("request_of_type", {
                        type: requestType
                    })
                });
                return;
            }
            if (data === undefined) {
                msg = "Undefined data for " + requestType;
                NRS.logConsole(msg);
                callback({
                    "errorCode": 1,
                    "errorDescription": msg
                });
                return;
            }
            if (callback === undefined) {
                NRS.logConsole("Undefined callback function for " + requestType);
                // We obviously can't invoke the callback here
                return;
            }

            for (const key in data) {
                if (key === "secretPhrase") {
                    continue;
                }
                if (!data.hasOwnProperty(key)) {
                    continue;
                }
                let val = data[key];
                if (typeof val === "string") {
                    data[key] = val.trim();
                }
            }

            let rc = performDataConversion(data);
            if (rc.errorCode) {
                callback(rc);
                return;
            }

            let hasAccountControl = requestType != "approveTransaction" && NRS.hasAccountControl();
            let hasAssetControl = NRS.isSubjectToAssetControl && NRS.isSubjectToAssetControl(requestType);
            if (hasAccountControl && hasAssetControl
                && (NRS.accountInfo.phasingOnly.controlParams.phasingVotingModel == NRS.constants.VOTING_MODELS.COMPOSITE
                    || NRS.getCurrentAssetControl().phasingVotingModel == NRS.constants.VOTING_MODELS.COMPOSITE)) {
                //User should have set the phasing manually
            } else if (hasAccountControl || hasAssetControl) {
                //Fill phasing parameters when account control or asset control are enabled
                let phasingControl;
                if (hasAccountControl) {
                    phasingControl = NRS.accountInfo.phasingOnly;
                    let maxFees = new BigInteger(phasingControl.maxFees);
                    if (maxFees > 0 && new BigInteger(data.feeNQT).compareTo(new BigInteger(phasingControl.maxFees)) > 0) {
                        callback({
                            "errorCode": 1,
                            "errorDescription": $.t("error_fee_exceeds_max_account_control_fee", {
                                "maxFee": NRS.convertToNXT(phasingControl.maxFees)
                            })
                        });
                        return;
                    }
                    let phasingDuration = parseInt(data.phasingFinishHeight) - NRS.lastBlockHeight;
                    let minDuration = parseInt(phasingControl.minDuration) > 0 ? parseInt(phasingControl.minDuration) : 0;
                    let maxDuration = parseInt(phasingControl.maxDuration) > 0 ? parseInt(phasingControl.maxDuration) : NRS.constants.SERVER.maxPhasingDuration;

                    if (phasingDuration < minDuration || phasingDuration > maxDuration) {
                        callback({
                            "errorCode": 1,
                            "errorDescription": $.t("error_finish_height_out_of_account_control_interval", {
                                "min": NRS.lastBlockHeight + minDuration,
                                "max": NRS.lastBlockHeight + maxDuration
                            })
                        });
                        return;
                    }
                    if (hasAssetControl) {
                        //Build composite phasing that satisfies both controls
                        let compositePhasingParameters = {
                            "phasingHolding": "0",
                            "phasingQuorum": 1,
                            "phasingMinBalance": 0,
                            "phasingMinBalanceModel": 0,
                            "phasingExpression": "ACC&ASC",
                            "phasingSubPolls": {
                                "ACC": phasingControl.controlParams,
                                "ASC": NRS.getCurrentAssetControl()
                            },
                            "phasingVotingModel": 6
                        };
                        data.phasingParams = JSON.stringify(compositePhasingParameters);
                    } else {
                        data.phasingParams = JSON.stringify(phasingControl.controlParams);
                    }
                } else {
                    data.phasingParams = JSON.stringify(NRS.getCurrentAssetControl());
                }

                data.phased = true;

                delete data.phasingHashedSecret;
                delete data.phasingHashedSecretAlgorithm;
                delete data.phasingLinkedFullHash;
                delete data.phasingLinkedTransaction;
            }

            if (!data.recipientPublicKey) {
                delete data.recipientPublicKey;
            }
            if (!data.referencedTransactionFullHash) {
                delete data.referencedTransactionFullHash;
            }

            // get account id from passphrase then call getAccount so that the passphrase is not submitted to the server
            if (requestType == "getAccountId") {
                let accountId = NRS.getAccountId(data.privateKey);
                NRS.sendRequest("getAccount", {account: accountId}, function (response) {
                    callback(response);
                });
                return;
            }

            if (!data.chain && !data.nochain) {
                data.chain = NRS.getActiveChainId();
            } else {
                delete data.nochain;
            }

            //check to see if secretPhrase supplied matches logged in account, if not - show error.
            if ("secretPhrase" in data) {
                let privateKey;
                let accountId;
                if (NRS.rememberPassword) {
                    privateKey = sessionPrivateKey;
                    accountId = NRS.getAccountId(privateKey);
                } else {
                    if (data.secretPhrase !== "") {
                        privateKey = NRS.getPrivateKey(data.secretPhrase);
                        accountId = NRS.getAccountId(privateKey);
                        delete data.secretPhrase;
                        data["privateKey"] = privateKey;
                    }
                }
                if (accountId && accountId != NRS.account && !data.isVoucher && !NRS.isPrivateKeyStoredOnHardware()) {
                    if (!data.privateKey) {
                        callback({
                            "errorCode": 1,
                            "errorDescription": $.t("error_passphrase_not_specified")
                        });
                        return;
                    } else {
                        callback({
                            "errorCode": 1,
                            "errorDescription": $.t("error_passphrase_incorrect_v2", {account: NRS.getAccountId(privateKey, true)})
                        });
                        return;
                    }
                }
            }
            NRS.processAjaxRequest(requestType, data, callback, options);
        };

        NRS.processAjaxRequest = function (requestType, data, callback, options) {
            let extra = {};
            if (data["_extra"]) {
                extra = data["_extra"];
                delete data["_extra"];
            }

            // Means it is a page request, not a global request. Currently we do not use this information here.
            let plusCharacter = requestType.indexOf("+");
            if (plusCharacter > 0) {
                requestType = requestType.substr(0, plusCharacter);
            }

            if (data.referencedTransactionFullHash) {
                if (!/^[a-z0-9]{64}$/.test(data.referencedTransactionFullHash)) {
                    callback({
                        "errorCode": -1,
                        "errorDescription": $.t("error_invalid_referenced_transaction_hash")
                    }, data);
                    return;
                }
            }

            let privateKey = "";
            let isVolatile = isVolatileRequest(data, requestType);
            if (isVolatile) {
                if (NRS.rememberPassword) {
                    privateKey = sessionPrivateKey;
                } else {
                    privateKey = data.privateKey ? data.privateKey : "";
                    if (data.isVoucher) {
                        extra.voucherPrivateKey = privateKey;
                    }
                }
                // Delete secrets from the submitted data and only use it to sign the response locally
                delete data.secretPhrase;
                delete data.privateKey;

                if (NRS.accountInfo && NRS.accountInfo.publicKey) {
                    data.publicKey = NRS.accountInfo.publicKey;
                } else if (!data.doNotSign && privateKey) {
                    data.publicKey = NRS.generatePublicKey(privateKey);
                    NRS.accountInfo.publicKey = data.publicKey;
                } else if (NRS.isPublicKeyLoadedOnLogin()) {
                    data.publicKey = NRS.publicKey;
                }
                let ecBlock = NRS.constants.LAST_KNOWN_BLOCK;
                data.ecBlockId = ecBlock.id;
                data.ecBlockHeight = ecBlock.height;
            } else if (NRS.rememberPassword) {
                data.privateKey = sessionPrivateKey;
            }

            let formData = null;
            let config = NRS.getFileUploadConfig(requestType, data);
            let file;
            if (config && !config.doNotSubmit && config.selector && config.selector.length > 0 && $(config.selector)[0] && $(config.selector)[0].files[0]) {
                formData = new FormData();
                if (data.messageFile) {
                    file = data.messageFile;
                    delete data.messageFile;
                    delete data.encrypt_message;
                } else {
                    file = $(config.selector)[0].files[0];
                }
                if (!file && requestType == "uploadTaggedData") {
                    callback({
                        "errorCode": 3,
                        "errorDescription": $.t("error_no_file_chosen")
                    }, data);
                    return;
                }
                if (file && file.size > config.maxSize) {
                    callback({
                        "errorCode": 3,
                        "errorDescription": $.t(config.errorDescription, {
                            "size": file.size,
                            "allowed": config.maxSize
                        })
                    }, data);
                    return;
                }
                formData.append(config.requestParam, file);
                for (let key in data) {
                    if (!data.hasOwnProperty(key)) {
                        continue;
                    }
                    if (data[key] instanceof Array) {
                        for (let i = 0; i < data[key].length; i++) {
                            formData.append(key, data[key][i]);
                        }
                    } else {
                        formData.append(key, data[key]);
                    }
                }
            }
            let url;
            if (options.url) {
                url = options.url;
            } else {
                url = NRS.getRequestPath(options.noProxy);
            }
            url += "?requestType=" + requestType;

            let currentRequestId = NRS.requestId++;
            if (NRS.isLogConsole(10)) {
                NRS.logConsole("Send request " + requestType + " to url " + url + " id=" + currentRequestId);
            }

            let fetchParams = {
                method: "POST",
                mode: "cors",
                cache: "no-cache",
            };
            if (formData !== null) {
                fetchParams.body = formData;
            } else {
                fetchParams.headers = {
                    "Content-Type": "application/x-www-form-urlencoded"
                };
                const body = new URLSearchParams();
                for (const key in data) {
                    if (!data.hasOwnProperty(key)) {
                        continue;
                    }
                    const value = data[key];
                    if (Array.isArray(value)) {
                        for (const arrayValue of value) {
                            body.append(key, arrayValue);
                        }
                    } else {
                        body.append(key, data[key]);
                    }
                }
                fetchParams.body = body;
            }
            fetch(url, fetchParams).then((response) => {
                if (response.ok) {
                    return response.text();
                }
                throw {status: response.status, message: "Http response status"};
            }).then((responseText) => {
                try {
                    let responseJson = JSON.parse(String(responseText));
                    processResponse(data, extra, options, requestType, privateKey, callback, file, isVolatile)(responseJson);
                } catch (e) {
                    let msg = "Failed to parse response as JSON";
                    NRS.logConsole(msg + ":" + responseText);
                    throw {status: 0, message: msg};
                }
            }).catch((error) => {
                let status;
                let message;
                if (typeof error === "string") {
                    status = 0;
                    message = error;
                } else if (typeof error === "object") {
                    NRS.logConsole(JSON.stringify(error));
                    status = error.status || 0;
                    message = error.message || "unknown";
                } else {
                    NRS.logConsole("Response error: " + error);
                    message = "unknown";
                    status = 0;
                }
                return processFailedResponse(options, requestType, currentRequestId, url, callback)(status, message);
            });
        };

        function processResponse(data, extra, options, requestType, privateKey, callback, file, isVolatile) {
            return async function (response) {
                if (!options.doNotEscape) {
                    NRS.escapeResponseObjStrings(response, ["transactionJSON"]);
                }
                if (NRS.console) {
                    NRS.addToConsole(this.url, this.type, this.data, response);
                }
                addAddressData(data);
                if ((privateKey !== undefined && privateKey !== "" || NRS.isHardwareTokenSigningEnabled()) && isResponseReadyForSigning(response, data, requestType)) {
                    let unsignedTransactionBytes = response.unsignedTransactionBytes;
                    let signingPromise = NRS.getSigningPromise(unsignedTransactionBytes, privateKey);
                    signingPromise.then(async function (signature) {
                        let publicKey = NRS.isPublicKeyLoadedOnLogin() ? NRS.publicKey : NRS.generatePublicKey(privateKey);
                        if (signature == null) {
                            callback({
                                "errorCode": 1,
                                "errorDescription": $.t("transaction_signing_cancelled")
                            }, data);
                            return;
                        }
                        if (!NRS.verifySignature(signature, unsignedTransactionBytes, publicKey, callback)) {
                            return;
                        }
                        addMissingData(data);
                        if (file && NRS.isFileReaderSupported()) {
                            data.filebytes = await NRS.readFileAsync(file);
                            data.filename = file.name;
                        }
                        NRS.verifyAndBroadcast(signature, requestType, data, callback, response, extra, isVolatile);
                    });
                } else {
                    if (response.errorCode || response.errorDescription || response.errorMessage || response.error) {
                        response.errorDescription = NRS.translateServerError(response);
                        delete response.fullHash;
                        if (!response.errorCode) {
                            response.errorCode = -1;
                        }
                        callback(response, data);
                    } else {
                        if (response.broadcasted == false && !data.calculateFee) {
                            addMissingData(data);
                            if (file && NRS.isFileReaderSupported()) {
                                data.filebytes = await NRS.readFileAsync(file);
                                data.filename = file.name;
                            }
                            if (response.unsignedTransactionBytes) {
                                let result = NRS.verifyTransactionBytes(response.unsignedTransactionBytes, requestType, data, response.transactionJSON.attachment, isVolatile);
                                if (result.fail) {
                                    NRS.logConsole("Could not validate unsigned bytes returned by the server. Parameter " + result.param + " expected data: " + result.expected + ", actual data " + result.actual);
                                    callback({
                                        "errorCode": 1,
                                        "errorDescription": $.t("error_bytes_validation_server_v2", {
                                            param: result.param,
                                            expected: result.expected,
                                            actual: result.actual
                                        })
                                    }, data);
                                    return;
                                }
                            }
                            NRS.showRawTransactionModal(response);
                        } else {
                            if (extra) {
                                data["_extra"] = extra;
                            }
                            callback(response, data);
                            if (data.referencedTransactionFullHash && !response.errorCode) {
                                $.growl($.t("info_referenced_transaction_hash"), {
                                    "type": "info"
                                });
                            }
                        }
                    }
                }
            };
        }

        function processFailedResponse(options, requestType, currentRequestId, url, callback) {
            return function (status, error) {
                NRS.logConsole("Request " + url + " processing failed for request type " + requestType +
                    " error " + error + " status " + status + " id " + currentRequestId);
                if (status >= 400) {
                    NRS.connectionError();
                }

                if (error == "timeout") {
                    error = $.t("error_request_timeout");
                }
                callback({
                    "errorCode": -1,
                    "errorDescription": error
                }, {});
            };
        }

        NRS.verifyAndBroadcast = function (signature, requestType, data, callback, response, extra, isVerifyECBlock) {
            let unsignedTransactionBytes = response.unsignedTransactionBytes;
            let result = NRS.verifyTransactionBytes(unsignedTransactionBytes, requestType, data, response.transactionJSON.attachment, isVerifyECBlock);
            if (result.fail) {
                NRS.logConsole("Could not validate unsigned bytes returned by the server. Parameter " + result.param + " expected data: " + result.expected + ", actual data " + result.actual);
                callback({
                    "errorCode": 1,
                    "errorDescription": $.t("error_bytes_validation_server_v2", {
                        param: result.param,
                        expected: result.expected,
                        actual: result.actual
                    })
                }, data);
                return;
            }

            let signedTransactionBytes = NRS.insertSignature(unsignedTransactionBytes, signature);
            if (data.broadcast == "false") {
                response.transactionBytes = signedTransactionBytes;
                response.transactionJSON.signature = signature;
                NRS.logConsole("before showRawTransactionModal data.broadcast == false");
                if (data.isVoucher) {
                    let signingPromise = NRS.getSigningPromise(response.unsignedTransactionBytes, extra.voucherPrivateKey);
                    signingPromise.then(function (signature) {
                        let publicKey = NRS.getPublicKeyFromPrivateKey(extra.voucherPrivateKey);
                        delete extra.voucherPrivateKey;
                        NRS.showVoucherModal(response, signature, publicKey, requestType);
                    });
                } else {
                    NRS.showRawTransactionModal(response);
                }
            } else {
                if (extra) {
                    data["_extra"] = extra;
                }
                NRS.broadcastTransactionBytes(signedTransactionBytes, callback, response, data, response.transactionJSON.attachment);
            }
        };

        NRS.verifyTransactionBytes = function (transactionBytes, requestType, data, attachment, isVerifyECBlock) {
            try {
                let byteArray = converters.hexStringToByteArray(transactionBytes);
                let transaction = {};
                let pos = 0;
                transaction.chain = String(converters.byteArrayToSignedInt32(byteArray, pos));
                pos += 4;
                transaction.type = byteArray[pos++];
                // Patch until I find the official way of converting JS byte to signed byte
                if (transaction.type >= 128) {
                    transaction.type -= 256;
                }
                transaction.subtype = byteArray[pos++];
                transaction.version = byteArray[pos++];
                transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, pos));
                pos += 4;
                transaction.deadline = String(converters.byteArrayToSignedShort(byteArray, pos));
                pos += 2;
                transaction.publicKey = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                pos += 32;
                transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, pos));
                pos += 8;
                transaction.amountNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                pos += 8;
                transaction.feeNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                pos += 8;
                transaction.signature = byteArray.slice(pos, pos + 64);
                pos += 64;
                transaction.ecBlockHeight = String(converters.byteArrayToSignedInt32(byteArray, pos));
                pos += 4;
                transaction.ecBlockId = String(converters.byteArrayToBigInteger(byteArray, pos));
                pos += 8;
                transaction.flags = String(converters.byteArrayToSignedInt32(byteArray, pos));
                pos += 4;
                if (isVerifyECBlock) {
                    let ecBlock = NRS.constants.LAST_KNOWN_BLOCK;
                    if (transaction.ecBlockHeight != ecBlock.height) {
                        return {
                            fail: true,
                            param: "ecBlockHeight",
                            actual: transaction.ecBlockHeight,
                            expected: ecBlock.height
                        };
                    }
                    if (transaction.ecBlockId != ecBlock.id) {
                        return {fail: true, param: "ecBlockId", actual: transaction.ecBlockId, expected: ecBlock.id};
                    }
                }

                if (transaction.chain != data.chain && !(transaction.chain == "1" && data.isParentChainTransaction == "1")) {
                    return {fail: true, param: "chain", actual: transaction.chain, expected: data.chain};
                }

                if (transaction.publicKey != NRS.accountInfo.publicKey && transaction.publicKey != data.publicKey && transaction.publicKey != data.senderPublicKey) {
                    return {
                        fail: true,
                        param: "publicKey",
                        actual: transaction.publicKey,
                        expected: NRS.accountInfo.publicKey || data.publicKey
                    };
                }

                if (transaction.deadline != data.deadline) {
                    return {fail: true, param: "deadline", actual: transaction.deadline, expected: data.deadline};
                }

                if (transaction.recipient !== data.recipient) {
                    if (!((data.recipient === undefined || data.recipient == "") && transaction.recipient == "0")) {
                        if (!NRS.isSpecialRecipient(requestType)) {
                            return {
                                fail: true,
                                param: "recipient",
                                actual: transaction.recipient,
                                expected: data.recipient
                            };
                        }
                    }
                }

                if (transaction.amountNQT !== data.amountNQT && !(requestType === "exchangeCoins" && transaction.amountNQT === "0")) {
                    return {fail: true, param: "amountNQT", actual: transaction.amountNQT, expected: data.amountNQT};
                }

                if ("referencedTransactionFullHash" in data) {
                    if (transaction.referencedTransactionFullHash !== data.referencedTransactionFullHash) {
                        return {
                            fail: true,
                            param: "referencedTransactionFullHash",
                            actual: transaction.referencedTransactionFullHash,
                            expected: data.referencedTransactionFullHash
                        };
                    }
                } else if (transaction.referencedTransactionFullHash && transaction.referencedTransactionFullHash !== "") {
                    return {
                        fail: true,
                        param: "referencedTransactionFullHash",
                        actual: transaction.referencedTransactionFullHash,
                        expected: ""
                    };
                }
                //has empty attachment, so no attachmentVersion byte...
                if (!(requestType == "sendMoney" || requestType == "sendMessage")) {
                    pos++;
                }
                return NRS.verifyTransactionTypes(byteArray, transaction, requestType, data, pos, attachment);
            } catch (e) {
                NRS.logException(e);
                return {fail: true, param: "exception", actual: e.message, expected: ""};
            }
        };

        NRS.verifyTransactionTypes = function (byteArray, transaction, requestType, data, pos, attachment) {
            let length = 0;
            let i = 0;
            let serverHash, sha256, utfBytes, isText, hashWords, calculatedHash, result, permissionType,
                permissionTypeEnumToServerIndex, typeStr, chain;
            let notOfTypeError = {
                fail: true,
                param: "requestType",
                actual: requestType,
                expected: "type:" + transaction.type + ", subtype:" + transaction.subtype
            };
            let verificationResult = null;
            let fields = [];

            function verificationFailed(param, actual, expected) {
                return {
                    fail: true,
                    param: param,
                    actual: actual,
                    expected: expected
                };
            }

            function verifyFields(transaction, data, fieldNames) {
                for (let fieldName of fieldNames) {
                    if (transaction[fieldName] !== String(data[fieldName])) {
                        return verificationFailed(fieldName, data[fieldName], transaction[fieldName]);
                    }
                }
                return {fail: false};
            }

            switch (requestType) {
                case "sendMoney":
                    if (NRS.notOfType(transaction, "FxtPayment") && NRS.notOfType(transaction, "OrdinaryPayment")) {
                        return notOfTypeError;
                    }
                    break;
                case "sendMessage":
                    if (NRS.notOfType(transaction, "ArbitraryMessage")) {
                        return notOfTypeError;
                    }
                    break;
                case "setAlias":
                    if (NRS.notOfType(transaction, "AliasAssignment")) {
                        return notOfTypeError;
                    }
                    length = parseInt(byteArray[pos], 10);
                    pos++;
                    transaction.aliasName = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.aliasURI = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    if (transaction.aliasName !== data.aliasName || transaction.aliasURI !== data.aliasURI) {
                        return {
                            fail: true,
                            param: requestType,
                            actual: data.aliasName + "/" + data.aliasURI,
                            expected: transaction.aliasName + "/" + transaction.aliasURI
                        };
                    }
                    break;
                case "createPoll":
                    if (NRS.notOfType(transaction, "PollCreation")) {
                        return notOfTypeError;
                    }
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.name = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.description = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    transaction.finishHeight = converters.byteArrayToSignedInt32(byteArray, pos);
                    pos += 4;
                    let nr_options = byteArray[pos];
                    pos++;

                    for (i = 0; i < nr_options; i++) {
                        let optionLength = converters.byteArrayToSignedShort(byteArray, pos);
                        pos += 2;
                        transaction["option" + (i < 10 ? "0" + i : i)] = converters.byteArrayToString(byteArray, pos, optionLength);
                        pos += optionLength;
                    }
                    transaction.votingModel = String(byteArray[pos]);
                    pos++;
                    transaction.minNumberOfOptions = String(byteArray[pos]);
                    pos++;
                    transaction.maxNumberOfOptions = String(byteArray[pos]);
                    pos++;
                    transaction.minRangeValue = String(byteArray[pos]);
                    pos++;
                    transaction.maxRangeValue = String(byteArray[pos]);
                    pos++;
                    transaction.minBalance = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.minBalanceModel = String(byteArray[pos]);
                    pos++;
                    transaction.holding = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['name', 'description', 'minNumberOfOptions', 'maxNumberOfOptions']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    for (i = 0; i < nr_options; i++) {
                        let paramName = "option" + (i < 10 ? "0" + i : i);
                        if (transaction[paramName] !== data[paramName]) {
                            return verificationFailed(paramName, data[paramName], transaction[paramName]);
                        }
                    }
                    let nextOptionKey = "option" + (i < 10 ? "0" + i : i);
                    if (nextOptionKey in data) {
                        return verificationFailed(nextOptionKey, data[nextOptionKey], undefined);
                    }
                    break;
                case "castVote":
                    if (NRS.notOfType(transaction, "VoteCasting")) {
                        return notOfTypeError;
                    }
                    transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    let voteLength = byteArray[pos];
                    pos++;
                    transaction.votes = [];

                    for (i = 0; i < voteLength; i++) {
                        transaction["vote" + (i < 10 ? "0" + i : i)] = byteArray[pos];
                        pos++;
                        // TODO validate vote bytes against data
                    }
                    if (transaction.poll !== String(data.poll)) {
                        return verificationFailed("poll", data.poll, transaction.poll);
                    }
                    break;
                case "setAccountInfo":
                    if (NRS.notOfType(transaction, "AccountInfo")) {
                        return notOfTypeError;
                    }
                    length = parseInt(byteArray[pos], 10);
                    pos++;
                    transaction.name = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.description = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    verificationResult = verifyFields(transaction, data, ['name', 'description']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "sellAlias":
                    if (NRS.notOfType(transaction, "AliasSell")) {
                        return notOfTypeError;
                    }
                    length = parseInt(byteArray[pos], 10);
                    pos++;
                    transaction.alias = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.alias !== data.aliasName) {
                        return verificationFailed('alias', data.aliasName, transaction.alias);
                    }
                    if (transaction.priceNQT !== String(data.priceNQT)) {
                        return verificationFailed('priceNQT', data.priceNQT, transaction.priceNQT);
                    }
                    break;
                case "buyAlias":
                    if (NRS.notOfType(transaction, "AliasBuy")) {
                        return notOfTypeError;
                    }
                    length = parseInt(byteArray[pos], 10);
                    pos++;
                    transaction.alias = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    if (transaction.alias !== data.aliasName) {
                        return verificationFailed('alias', data.aliasName, transaction.alias);
                    }
                    break;
                case "deleteAlias":
                    if (NRS.notOfType(transaction, "AliasDelete")) {
                        return notOfTypeError;
                    }
                    length = parseInt(byteArray[pos], 10);
                    pos++;
                    transaction.alias = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    if (transaction.alias !== data.aliasName) {
                        return verificationFailed('alias', data.aliasName, transaction.alias);
                    }
                    break;
                case "approveTransaction":
                    if (NRS.notOfType(transaction, "PhasingVoteCasting")) {
                        return notOfTypeError;
                    }
                    let fullHashesLength = byteArray[pos];
                    pos++;
                    if (fullHashesLength > 1) {
                        return {
                            fail: true,
                            param: requestType + "fullHashesLength",
                            actual: fullHashesLength,
                            expected: 1
                        };
                    }
                    if (fullHashesLength === 1) {
                        let phasedTransaction = converters.byteArrayToSignedInt32(byteArray, pos);
                        pos += 4;
                        phasedTransaction += ":" + converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                        pos += 32;
                        if (phasedTransaction !== data.phasedTransaction) {
                            return {
                                fail: true,
                                param: requestType + "PhasedTransaction",
                                actual: JSON.stringify(data),
                                expected: JSON.stringify(transaction)
                            };
                        }
                    }
                    let numberOfSecrets = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    if (numberOfSecrets < 0 || numberOfSecrets > 1
                        || numberOfSecrets == 0 && (data.revealedSecretText && data.revealedSecretText !== "" || data.revealedSecret && data.revealedSecret !== "")
                        || numberOfSecrets == 1 && (!data.revealedSecretText || data.revealedSecretText === "") && (!data.revealedSecret || data.revealedSecret === "")) {
                        return {
                            fail: true,
                            param: requestType + "NumberOfSecrets",
                            actual: JSON.stringify(data),
                            expected: numberOfSecrets
                        };
                    }
                    // We only support one secret per phasing model
                    if (numberOfSecrets === 1) {
                        transaction.revealedSecretLength = converters.byteArrayToSignedShort(byteArray, pos);
                        pos += 2;
                        if (transaction.revealedSecretLength > 0) {
                            transaction.revealedSecret = converters.byteArrayToHexString(byteArray.slice(pos, pos + transaction.revealedSecretLength));
                            pos += transaction.revealedSecretLength;
                        } else {
                            transaction.revealedSecret = "";
                        }
                        if (transaction.revealedSecret !== data.revealedSecret &&
                            transaction.revealedSecret !== converters.byteArrayToHexString(NRS.getUtf8Bytes(data.revealedSecretText))) {
                            return {
                                fail: true,
                                param: requestType + "RevealedSecret",
                                actual: JSON.stringify(data),
                                expected: JSON.stringify(transaction)
                            };
                        }
                    }
                    break;
                case "setAccountProperty":
                    if (NRS.notOfType(transaction, "AccountProperty")) {
                        return notOfTypeError;
                    }
                    length = byteArray[pos];
                    pos++;
                    if (converters.byteArrayToString(byteArray, pos, length) !== data.property) {
                        return {
                            fail: true,
                            param: requestType + "Key",
                            actual: JSON.stringify(data),
                            expected: JSON.stringify(transaction)
                        };
                    }
                    pos += length;
                    length = byteArray[pos];
                    pos++;
                    if (converters.byteArrayToString(byteArray, pos, length) !== data.value) {
                        return {
                            fail: true,
                            param: requestType + "Value",
                            actual: JSON.stringify(data),
                            expected: JSON.stringify(transaction)
                        };
                    }
                    pos += length;
                    break;
                case "deleteAccountProperty":
                    if (NRS.notOfType(transaction, "AccountPropertyDelete")) {
                        return notOfTypeError;
                    }
                    // no way to validate the property id, just skip it
                    String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    break;
                case "issueAsset":
                    if (NRS.notOfType(transaction, "AssetIssuance")) {
                        return notOfTypeError;
                    }
                    length = byteArray[pos];
                    pos++;
                    transaction.name = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.description = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.decimals = String(byteArray[pos]);
                    pos++;
                    verificationResult = verifyFields(transaction, data, ['name', 'description', 'quantityQNT', 'decimals']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "transferAsset":
                    if (NRS.notOfType(transaction, "AssetTransfer")) {
                        return notOfTypeError;
                    }
                    transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.asset !== data.asset) {
                        return verificationFailed('asset', data.asset, transaction.asset);
                    }
                    if (transaction.quantityQNT !== String(data.quantityQNT)) {
                        return verificationFailed('quantityQNT', data.quantityQNT, transaction.quantityQNT);
                    }
                    break;
                case "placeAskOrder":
                case "placeBidOrder":
                    if (NRS.notOfType(transaction, "AskOrderPlacement") && NRS.notOfType(transaction, "BidOrderPlacement")) {
                        return notOfTypeError;
                    }
                    transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.priceNQTPerShare = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['asset', 'priceNQTPerShare', 'quantityQNT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "cancelAskOrder":
                case "cancelBidOrder":
                    if (NRS.notOfType(transaction, "AskOrderCancellation") && NRS.notOfType(transaction, "BidOrderCancellation")) {
                        return notOfTypeError;
                    }
                    transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.order !== data.order) {
                        return verificationFailed('order', data.order, transaction.order);
                    }
                    break;
                case "deleteAssetShares":
                    if (NRS.notOfType(transaction, "AssetDelete")) {
                        return notOfTypeError;
                    }
                    transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.asset !== data.asset) {
                        return verificationFailed('asset', data.asset, transaction.asset);
                    }
                    if (transaction.quantityQNT !== data.quantityQNT) {
                        return verificationFailed('quantityQNT', data.quantityQNT, transaction.quantityQNT);
                    }
                    break;
                case "increaseAssetShares":
                    if (NRS.notOfType(transaction, "AssetIncrease")) {
                        return notOfTypeError;
                    }
                    transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.asset !== data.asset) {
                        return verificationFailed('asset', data.asset, transaction.asset);
                    }
                    if (transaction.quantityQNT !== data.quantityQNT) {
                        return verificationFailed('quantityQNT', data.quantityQNT, transaction.quantityQNT);
                    }
                    break;
                case "dividendPayment":
                    if (NRS.notOfType(transaction, "DividendPayment")) {
                        return notOfTypeError;
                    }
                    transaction.holding = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.holdingType = String(byteArray[pos]);
                    pos++;
                    transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.height = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    transaction.amountNQTPerShare = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['holding', 'holdingType', 'asset', 'height', 'amountNQTPerShare']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "setPhasingAssetControl":
                    if (NRS.notOfType(transaction, "SetPhasingAssetControl")) {
                        return notOfTypeError;
                    }
                    if (data.asset !== String(converters.byteArrayToBigInteger(byteArray, pos))) {
                        return {
                            fail: true,
                            param: requestType + "Asset",
                            actual: JSON.stringify(data),
                            expected: String(converters.byteArrayToBigInteger(byteArray, pos))
                        };
                    }
                    pos += 8;
                    result = validateControlPhasingData(data, byteArray, pos, false);
                    if (result.fail) {
                        return result;
                    } else {
                        pos = result.pos;
                    }
                    break;
                case "setAssetProperty":
                    if (NRS.notOfType(transaction, "AssetProperty")) {
                        return notOfTypeError;
                    }
                    if (data.asset !== String(converters.byteArrayToBigInteger(byteArray, pos))) {
                        return {
                            fail: true,
                            param: requestType + "Asset",
                            actual: JSON.stringify(data),
                            expected: String(converters.byteArrayToBigInteger(byteArray, pos))
                        };
                    }
                    pos += 8;
                    length = byteArray[pos];
                    pos++;
                    if (converters.byteArrayToString(byteArray, pos, length) !== data.property) {
                        return {
                            fail: true,
                            param: requestType + "Key",
                            actual: JSON.stringify(data),
                            expected: JSON.stringify(transaction)
                        };
                    }
                    pos += length;
                    length = byteArray[pos];
                    pos++;
                    if (converters.byteArrayToString(byteArray, pos, length) !== data.value) {
                        return {
                            fail: true,
                            param: requestType + "Value",
                            actual: JSON.stringify(data),
                            expected: JSON.stringify(transaction)
                        };
                    }
                    pos += length;
                    break;
                case "deleteAssetProperty":
                    if (NRS.notOfType(transaction, "AssetPropertyDelete")) {
                        return notOfTypeError;
                    }
                    // no way to validate the property id, just skip it
                    String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    break;
                case "dgsListing":
                    if (NRS.notOfType(transaction, "DigitalGoodsListing")) {
                        return notOfTypeError;
                    }
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.name = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.description = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.tags = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['name', 'description', 'tags', 'quantity', 'priceNQT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "dgsDelisting":
                    if (NRS.notOfType(transaction, "DigitalGoodsDelisting")) {
                        return notOfTypeError;
                    }
                    transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.goods !== data.goods) {
                        return verificationFailed('goods', data.goods, transaction.goods);
                    }
                    break;
                case "dgsPriceChange":
                    if (NRS.notOfType(transaction, "DigitalGoodsPriceChange")) {
                        return notOfTypeError;
                    }
                    transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.goods !== data.goods) {
                        return verificationFailed('goods', data.goods, transaction.goods);
                    }
                    if (transaction.priceNQT !== data.priceNQT) {
                        return verificationFailed('priceNQT', data.priceNQT, transaction.priceNQT);
                    }
                    break;
                case "dgsQuantityChange":
                    if (NRS.notOfType(transaction, "DigitalGoodsQuantityChange")) {
                        return notOfTypeError;
                    }
                    transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.deltaQuantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    if (transaction.goods !== data.goods) {
                        return verificationFailed('goods', data.goods, transaction.goods);
                    }
                    if (transaction.deltaQuantity !== String(data.deltaQuantity)) {
                        return verificationFailed('deltaQuantity', data.deltaQuantity, transaction.deltaQuantity);
                    }
                    break;
                case "dgsPurchase":
                    if (NRS.notOfType(transaction, "DigitalGoodsPurchase")) {
                        return notOfTypeError;
                    }
                    transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.deliveryDeadlineTimestamp = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    verificationResult = verifyFields(transaction, data, ['goods', 'quantity', 'priceNQT', 'deliveryDeadlineTimestamp']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "dgsDelivery":
                    if (NRS.notOfType(transaction, "DigitalGoodsDelivery")) {
                        return notOfTypeError;
                    }
                    transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    let encryptedGoodsLength = converters.byteArrayToSignedShort(byteArray, pos);
                    let goodsLength = converters.byteArrayToSignedInt32(byteArray, pos);
                    transaction.goodsIsText = goodsLength < 0; // ugly hack??
                    if (goodsLength < 0) {
                        goodsLength &= NRS.constants.MAX_INT_JAVA;
                    }
                    pos += 4;
                    transaction.goodsData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedGoodsLength));
                    pos += encryptedGoodsLength;
                    transaction.goodsNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                    pos += 32;
                    transaction.discountNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    let goodsIsText = (transaction.goodsIsText ? "true" : "false");
                    if (goodsIsText != String(data.goodsIsText)) {
                        return verificationFailed('goodsIsText', data.goodsIsText, transaction.goodsIsText);
                    }
                    verificationResult = verifyFields(transaction, data, ['purchase', 'goodsData', 'goodsNonce', 'discountNQT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "dgsFeedback":
                    if (NRS.notOfType(transaction, "DigitalGoodsFeedback")) {
                        return notOfTypeError;
                    }
                    transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.purchase !== String(data.purchase)) {
                        return verificationFailed('purchase', data.purchase, transaction.purchase);
                    }
                    break;
                case "dgsRefund":
                    if (NRS.notOfType(transaction, "DigitalGoodsRefund")) {
                        return notOfTypeError;
                    }
                    transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.refundNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.purchase !== String(data.purchase)) {
                        return verificationFailed('purchase', data.purchase, transaction.purchase);
                    }
                    if (transaction.refundNQT !== data.refundNQT) {
                        return verificationFailed('refundNQT', data.refundNQT, transaction.refundNQT);
                    }
                    break;
                case "leaseBalance":
                    if (NRS.notOfType(transaction, "EffectiveBalanceLeasing")) {
                        return notOfTypeError;
                    }
                    transaction.period = String(converters.byteArrayToSignedShort(byteArray, pos));
                    pos += 2;
                    if (transaction.period !== String(data.period)) {
                        return verificationFailed('period', data.period, transaction.period);
                    }
                    break;
                case "setPhasingOnlyControl":
                    if (NRS.notOfType(transaction, "SetPhasingOnly")) {
                        return notOfTypeError;
                    }
                    result = validateControlPhasingData(data, byteArray, pos, true);
                    if (result.fail) {
                        return result;
                    } else {
                        pos = result.pos;
                    }
                    break;
                case "issueCurrency":
                    if (NRS.notOfType(transaction, "CurrencyIssuance")) {
                        return notOfTypeError;
                    }
                    length = byteArray[pos];
                    pos++;
                    transaction.name = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    let codeLength = byteArray[pos];
                    pos++;
                    transaction.code = converters.byteArrayToString(byteArray, pos, codeLength);
                    pos += codeLength;
                    length = converters.byteArrayToSignedShort(byteArray, pos);
                    pos += 2;
                    transaction.description = converters.byteArrayToString(byteArray, pos, length);
                    pos += length;
                    transaction.type = String(byteArray[pos]);
                    pos++;
                    transaction.initialSupplyQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.reserveSupplyQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.maxSupplyQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.issuanceHeight = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    transaction.minReservePerUnitNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.minDifficulty = String(byteArray[pos]);
                    pos++;
                    transaction.maxDifficulty = String(byteArray[pos]);
                    pos++;
                    transaction.ruleset = String(byteArray[pos]);
                    pos++;
                    transaction.algorithm = String(byteArray[pos]);
                    pos++;
                    transaction.decimals = String(byteArray[pos]);
                    pos++;
                    fields = ['name', 'code', 'description', 'type', 'initialSupplyQNT', 'maxSupplyQNT'];
                    verificationResult = verifyFields(transaction, data, fields);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    fields = ['issuanceHeight', 'reserveSupplyQNT', 'minReservePerUnitNQT', 'minDifficulty', 'maxDifficulty', 'ruleset', 'algorithm', 'decimals'];
                    for (let fieldName of fields) {
                        if (transaction[fieldName] !== "0" && transaction[fieldName] !== String(data[fieldName])) {
                            return verificationFailed(fieldName, data[fieldName], transaction[fieldName]);
                        }
                    }
                    break;
                case "currencyReserveIncrease":
                    if (NRS.notOfType(transaction, "ReserveIncrease")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.amountPerUnitNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['currency', 'amountPerUnitNQT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "currencyReserveClaim":
                    if (NRS.notOfType(transaction, "ReserveClaim")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.unitsQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['currency', 'unitsQNT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "transferCurrency":
                    if (NRS.notOfType(transaction, "CurrencyTransfer")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.unitsQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['currency', 'unitsQNT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "publishExchangeOffer":
                    if (NRS.notOfType(transaction, "PublishExchangeOffer")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.buyRateNQTPerUnit = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.sellRateNQTPerUnit = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.totalBuyLimitQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.totalSellLimitQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.initialBuySupplyQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.initialSellSupplyQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.expirationHeight = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    fields = ['currency', 'buyRateNQTPerUnit', 'sellRateNQTPerUnit', 'totalBuyLimitQNT',
                        'totalSellLimitQNT', 'initialBuySupplyQNT', 'initialSellSupplyQNT', 'expirationHeight'];
                    verificationResult = verifyFields(transaction, data, fields);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "currencyBuy":
                    if (NRS.notOfType(transaction, "ExchangeBuy")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.rateNQTPerUnit = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.unitsQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['currency', 'rateNQTPerUnit', 'unitsQNT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "currencySell":
                    if (NRS.notOfType(transaction, "ExchangeSell")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.rateNQTPerUnit = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.unitsQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['currency', 'rateNQTPerUnit', 'unitsQNT']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "currencyMint":
                    if (NRS.notOfType(transaction, "CurrencyMinting")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.nonce = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.unitsQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    transaction.counter = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    verificationResult = verifyFields(transaction, data, ['currency', 'nonce', 'unitsQNT', 'counter']);
                    if (verificationResult.fail) {
                        return verificationResult;
                    }
                    break;
                case "deleteCurrency":
                    if (NRS.notOfType(transaction, "CurrencyDeletion")) {
                        return notOfTypeError;
                    }
                    transaction.currency = String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (transaction.currency !== data.currency) {
                        return verificationFailed('currency', data.currency, transaction.currency);
                    }
                    break;
                case "uploadTaggedData":
                    if (NRS.notOfType(transaction, "TaggedDataUpload")) {
                        return notOfTypeError;
                    }
                    if (byteArray[pos] != 0) {
                        return {fail: true, param: requestType + "Pos", actual: JSON.stringify(data), expected: ""};
                    }
                    pos++;
                    serverHash = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                    pos += 32;
                    sha256 = CryptoJS.algo.SHA256.create();
                    utfBytes = NRS.getUtf8Bytes(data.name);
                    sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    utfBytes = NRS.getUtf8Bytes(data.description);
                    sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    utfBytes = NRS.getUtf8Bytes(data.tags);
                    sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    utfBytes = NRS.getUtf8Bytes(attachment.type);
                    sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    utfBytes = NRS.getUtf8Bytes(data.channel);
                    sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    isText = [];
                    if (attachment.isText) {
                        isText.push(1);
                    } else {
                        isText.push(0);
                    }
                    sha256.update(converters.byteArrayToWordArrayEx(isText));
                    utfBytes = NRS.getUtf8Bytes(data.filename);
                    sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    if (data.filebytes !== undefined) {
                        let dataBytes = new Int8Array(data.filebytes);
                        sha256.update(converters.byteArrayToWordArrayEx(dataBytes));
                    } else {
                        utfBytes = NRS.getUtf8Bytes(data.data);
                        sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                    }
                    hashWords = sha256.finalize();
                    calculatedHash = converters.byteArrayToHexString(converters.wordArrayToByteArrayEx(hashWords));
                    if (serverHash !== calculatedHash) {
                        return {
                            fail: true,
                            param: requestType + "ServerHash",
                            actual: serverHash,
                            expected: calculatedHash
                        };
                    }
                    break;
                case "shufflingCreate":
                    if (NRS.notOfType(transaction, "ShufflingCreation")) {
                        return notOfTypeError;
                    }
                    let holding = String(converters.byteArrayToBigInteger(byteArray, pos));
                    if (holding !== "0" && holding !== data.holding ||
                        holding === "0" && data.holding !== undefined && data.holding !== "" && data.holding !== "0") {
                        return {fail: true, param: 'holding', actual: holding, expected: data.holding};
                    }
                    pos += 8;
                    let holdingType = String(byteArray[pos]);
                    let dataHoldingType = String(data.holdingType);
                    if (holdingType !== "0" && holdingType !== dataHoldingType ||
                        holdingType === "0" && dataHoldingType !== undefined && dataHoldingType !== "" && dataHoldingType !== "0") {
                        return {fail: true, param: 'holdingType', actual: holdingType, expected: data.holdingType};
                    }
                    pos++;
                    let amount = String(converters.byteArrayToBigInteger(byteArray, pos));
                    if (amount !== data.amount) {
                        return {fail: true, param: 'amount', actual: amount, expected: data.amount};
                    }
                    pos += 8;
                    let participantCount = String(byteArray[pos]);
                    if (participantCount !== String(data.participantCount)) {
                        return verificationFailed('participantCount', data.participantCount, participantCount);
                    }
                    pos++;
                    let registrationPeriod = String(converters.byteArrayToSignedShort(byteArray, pos));
                    if (registrationPeriod !== String(data.registrationPeriod)) {
                        return verificationFailed('registrationPeriod', data.registrationPeriod, registrationPeriod);
                    }
                    pos += 2;
                    break;
                case "exchangeCoins":
                    typeStr = transaction.chain == "1" ? "FxtCoinExchangeOrderIssue" : "CoinExchangeOrderIssue";
                    if (NRS.notOfType(transaction, typeStr)) {
                        return notOfTypeError;
                    }
                    chain = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    if (chain != data.chain) {
                        return {fail: true, param: requestType, actual: chain, expected: data.chain};
                    }
                    pos += 4;
                    let exchange = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    if (exchange != data.exchange) {
                        return {fail: true, param: requestType, actual: exchange, expected: data.exchange};
                    }
                    pos += 4;
                    let quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
                    if (quantityQNT !== data.quantityQNT) {
                        return {fail: true, param: requestType, actual: quantityQNT, expected: data.quantityQNT};
                    }
                    pos += 8;
                    let priceNQTPerCoin = String(converters.byteArrayToBigInteger(byteArray, pos));
                    if (priceNQTPerCoin !== data.priceNQTPerCoin) {
                        return {
                            fail: true,
                            param: requestType,
                            actual: priceNQTPerCoin,
                            expected: data.priceNQTPerCoin
                        };
                    }
                    pos += 8;
                    break;
                case "cancelCoinExchange":
                    typeStr = transaction.chain == "1" ? "FxtCoinExchangeOrderCancel" : "CoinExchangeOrderCancel";
                    if (NRS.notOfType(transaction, typeStr)) {
                        return notOfTypeError;
                    }
                    let orderHash = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                    if (NRS.fullHashToId(orderHash) !== data.order) {
                        return {
                            fail: true,
                            param: requestType,
                            actual: NRS.fullHashToId(orderHash),
                            expected: data.order
                        };
                    }
                    pos += 32;
                    break;
                case "bundleTransactions":
                    if (NRS.notOfType(transaction, "ChildChainBlock")) {
                        return notOfTypeError;
                    }
                    let isPrunable = byteArray[pos];
                    if (isPrunable != 0) {
                        return {fail: true, param: requestType, actual: isPrunable, expected: 0};
                    }
                    pos++;
                    chain = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    if (chain != data.childChain) {
                        return {fail: true, param: requestType, actual: chain, expected: data.childChain};
                    }
                    pos += 4;
                    serverHash = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                    sha256 = CryptoJS.algo.SHA256.create();
                    // We assume here that only one transaction was bundled
                    sha256.update(converters.byteArrayToWordArrayEx(converters.hexStringToByteArray(data.transactionFullHash)));
                    hashWords = sha256.finalize();
                    calculatedHash = converters.wordArrayToByteArrayEx(hashWords);
                    if (serverHash !== converters.byteArrayToHexString(calculatedHash)) {
                        return {
                            fail: true,
                            param: requestType,
                            actual: converters.byteArrayToHexString(calculatedHash),
                            expected: serverHash
                        };
                    }
                    pos += 32;
                    break;
                case "addAccountPermission":
                    if (NRS.notOfType(transaction, "AddPermission")) {
                        return notOfTypeError;
                    }
                    permissionType = byteArray[pos];
                    permissionTypeEnumToServerIndex = Object.values(NRS.PERMISSIONS).indexOf(data.permission);
                    if (permissionType != permissionTypeEnumToServerIndex) {
                        return {
                            fail: true,
                            param: requestType,
                            actual: permissionType,
                            expected: permissionTypeEnumToServerIndex
                        };
                    }
                    break;
                case "removeAccountPermission":
                    if (NRS.notOfType(transaction, "RemovePermission")) {
                        return notOfTypeError;
                    }
                    permissionType = byteArray[pos];
                    permissionTypeEnumToServerIndex = Object.values(NRS.PERMISSIONS).indexOf(data.permission);
                    if (permissionType != permissionTypeEnumToServerIndex) {
                        return {
                            fail: true,
                            param: requestType,
                            actual: permissionType,
                            expected: permissionTypeEnumToServerIndex
                        };
                    }
                    break;
                default:
                    return notOfTypeError;
            }

            return NRS.verifyAppendix(byteArray, transaction, requestType, data, pos);
        };

        NRS.verifyAppendix = function (byteArray, transaction, requestType, data, pos) {
            let calculatedHash, hashWords, sha256, serverHash, attachmentVersion, flags, result;

            // MessageAppendix
            if ((transaction.flags & 1) != 0 ||
                (requestType == "sendMessage"
                    && data.message && !(data.messageIsPrunable === "true"))) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 2) {
                    return {
                        fail: true,
                        param: "MessageAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                flags = byteArray[pos];
                pos++;
                transaction.messageIsText = flags && 1;
                let messageIsText = (transaction.messageIsText ? "true" : "false");
                if (data.messageIsText !== undefined && messageIsText !== String(data.messageIsText)) {
                    return {fail: true, param: "MessageAppendix", actual: messageIsText, expected: data.messageIsText};
                }
                let messageLength = converters.byteArrayToSignedShort(byteArray, pos);
                if (messageLength < 0) {
                    messageLength &= NRS.constants.MAX_SHORT_JAVA;
                }
                pos += 2;
                if (transaction.messageIsText) {
                    transaction.message = converters.byteArrayToString(byteArray, pos, messageLength);
                } else {
                    let slice = byteArray.slice(pos, pos + messageLength);
                    transaction.message = converters.byteArrayToHexString(slice);
                }
                pos += messageLength;
                if (transaction.message !== data.message) {
                    return {fail: true, param: "MessageAppendix", actual: transaction.message, expected: data.message};
                }
            } else if (data.message && !(data.messageIsPrunable === "true" || data.messageHash)) {
                return {fail: true, param: "MessageAppendix", actual: "message", expected: ""};
            }

            // EncryptedMessageAppendix
            if ((transaction.flags & 2) != 0) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 2) {
                    return {
                        fail: true,
                        param: "EncryptedMessageAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                flags = byteArray[pos];
                pos++;
                transaction.messageToEncryptIsText = flags && 1;
                let messageToEncryptIsText = (transaction.messageToEncryptIsText ? "true" : "false");
                if (messageToEncryptIsText != String(data.messageToEncryptIsText) && transaction.messageToEncryptIsText != data.isText) {
                    return {
                        fail: true,
                        param: "EncryptedMessageAppendix",
                        actual: messageToEncryptIsText,
                        expected: data.messageToEncryptIsText
                    };
                }
                let encryptedMessageLength = converters.byteArrayToSignedShort(byteArray, pos);
                if (encryptedMessageLength < 0) {
                    encryptedMessageLength &= NRS.constants.MAX_SHORT_JAVA;
                }
                pos += 2;
                transaction.encryptedMessageData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedMessageLength));
                pos += encryptedMessageLength;
                transaction.encryptedMessageNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                pos += 32;
                if (transaction.encryptedMessageData !== (data.encryptedMessageData || data.data) || transaction.encryptedMessageNonce !== (data.encryptedMessageNonce || data.nonce)) {
                    return {
                        fail: true,
                        param: "EncryptedMessageAppendix",
                        actual: JSON.stringify(transaction),
                        expected: JSON.stringify(data)
                    };
                }
            } else if (data.encryptedMessageData && !(String(data.encryptedMessageIsPrunable) === "true")) {
                return {fail: true, param: "EncryptedMessageAppendix", actual: "", expected: JSON.stringify(data)};
            }

            // EncryptToSelfMessageAppendix
            if ((transaction.flags & 4) != 0) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 2) {
                    return {
                        fail: true,
                        param: "EncryptToSelfMessageAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                flags = byteArray[pos];
                pos++;
                transaction.messageToEncryptToSelfIsText = flags && 1;
                let messageToEncryptToSelfIsText = (transaction.messageToEncryptToSelfIsText ? "true" : "false");
                if (messageToEncryptToSelfIsText != String(data.messageToEncryptToSelfIsText)) {
                    return {
                        fail: true,
                        param: "EncryptToSelfMessageAppendix",
                        actual: messageToEncryptToSelfIsText,
                        expected: data.messageToEncryptToSelfIsText
                    };
                }
                let encryptedToSelfMessageLength = converters.byteArrayToSignedShort(byteArray, pos);
                if (encryptedToSelfMessageLength < 0) {
                    encryptedToSelfMessageLength &= NRS.constants.MAX_SHORT_JAVA;
                }
                pos += 2;
                transaction.encryptToSelfMessageData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedToSelfMessageLength));
                pos += encryptedToSelfMessageLength;
                transaction.encryptToSelfMessageNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                pos += 32;
                if (transaction.encryptToSelfMessageData !== data.encryptToSelfMessageData || transaction.encryptToSelfMessageNonce !== data.encryptToSelfMessageNonce) {
                    return {
                        fail: true,
                        param: "EncryptToSelfMessageAppendix",
                        actual: JSON.stringify(transaction),
                        expected: JSON.stringify(data)
                    };
                }
            } else if (data.encryptToSelfMessageData) {
                return {
                    fail: true,
                    param: "EncryptToSelfMessageAppendix",
                    actual: data.encryptToSelfMessageData,
                    expected: ""
                };
            }

            // PrunablePlainMessageAppendix
            let utfBytes;
            if ((transaction.flags & 8) != 0) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 2) {
                    return {
                        fail: true,
                        param: "PrunablePlainMessageAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                flags = byteArray[pos];
                pos++;
                if (flags != 0) {
                    return {
                        fail: true,
                        param: "PrunablePlainMessageAppendix",
                        actual: flags,
                        expected: JSON.stringify(data)
                    };
                }
                serverHash = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                pos += 32;
                sha256 = CryptoJS.algo.SHA256.create();

                // TODO this approach does not work since the server sets isText using detectMimeType, this means that when uploading text
                // files to a remote node the transaction bytes used to calcualte the hash cannot be verified unless we also detect the same
                // mime type in the browser.
                let isText = [];
                if (String(data.messageIsText) == "true") {
                    isText.push(1);
                } else {
                    isText.push(0);
                }
                sha256.update(converters.byteArrayToWordArrayEx(isText));
                if (data.filebytes) {
                    utfBytes = new Int8Array(data.filebytes);
                } else {
                    utfBytes = NRS.getUtf8Bytes(data.message);
                }
                sha256.update(converters.byteArrayToWordArrayEx(utfBytes));
                hashWords = sha256.finalize();
                calculatedHash = converters.wordArrayToByteArrayEx(hashWords);
                if (serverHash !== converters.byteArrayToHexString(calculatedHash)) {
                    return {
                        fail: true,
                        param: "PrunablePlainMessageAppendix",
                        actual: converters.byteArrayToHexString(calculatedHash),
                        expected: serverHash
                    };
                }
            }

            // PrunableEncryptedMessageAppendix
            if ((transaction.flags & 16) != 0) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 2) {
                    return {
                        fail: true,
                        param: "PrunableEncryptedMessageAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                flags = byteArray[pos];
                pos++;
                if (flags != 0) {
                    return {
                        fail: true,
                        param: "PrunableEncryptedMessageAppendix",
                        actual: flags,
                        expected: JSON.stringify(data)
                    };
                }
                serverHash = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                pos += 32;
                sha256 = CryptoJS.algo.SHA256.create();
                if (data.isText == true || data.messageToEncryptIsText == "true") {
                    sha256.update(converters.byteArrayToWordArrayEx([1]));
                } else {
                    sha256.update(converters.byteArrayToWordArrayEx([0]));
                }
                sha256.update(converters.byteArrayToWordArrayEx([1])); // compression
                let encryptedMessageData = data.encryptedMessageData || data.data;
                let bytes = converters.hexStringToByteArray(encryptedMessageData);
                sha256.update(converters.byteArrayToWordArrayEx(bytes));
                let messageNonce = data.encryptedMessageNonce || data.nonce;
                sha256.update(converters.byteArrayToWordArrayEx(converters.hexStringToByteArray(messageNonce)));
                hashWords = sha256.finalize();
                calculatedHash = converters.wordArrayToByteArrayEx(hashWords);
                if (serverHash !== converters.byteArrayToHexString(calculatedHash)) {
                    return {
                        fail: true,
                        param: "PrunableEncryptedMessageAppendix",
                        actual: converters.byteArrayToHexString(calculatedHash),
                        expected: serverHash
                    };
                }
            }

            // PublicKeyAnnouncementAppendix
            if ((transaction.flags & 32) != 0) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 2) {
                    return {
                        fail: true,
                        param: "PublicKeyAnnouncementAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                let recipientPublicKey = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
                if (recipientPublicKey != data.recipientPublicKey) {
                    return {
                        fail: true,
                        param: "PublicKeyAnnouncementAppendix",
                        actual: recipientPublicKey,
                        expected: data.recipientPublicKey
                    };
                }
                pos += 32;
            } else if (data.recipientPublicKey) {
                return {
                    fail: true,
                    param: "PublicKeyAnnouncementAppendix",
                    actual: data.recipientPublicKey,
                    expected: "undefined"
                };
            }

            // PhasingAppendix
            if ((transaction.flags & 64) != 0) {
                attachmentVersion = byteArray[pos];
                if (attachmentVersion < 0 || attachmentVersion > 1) {
                    return {
                        fail: true,
                        param: "PhasingAppendix",
                        actual: attachmentVersion,
                        expected: JSON.stringify(data)
                    };
                }
                pos++;
                if (String(converters.byteArrayToSignedInt32(byteArray, pos)) !== data.phasingFinishHeight) {
                    return {
                        fail: true,
                        param: "PhasingAppendix",
                        actual: String(converters.byteArrayToSignedInt32(byteArray, pos)),
                        expected: data.phasingFinishHeight
                    };
                }
                pos += 4;
                let params = JSON.parse(data["phasingParams"]);
                result = validateCommonPhasingData(byteArray, pos, params);
                if (result.fail) {
                    return result;
                } else {
                    pos = result.pos;
                }
                if (params.phasingVotingModel == NRS.constants.VOTING_MODELS.TRANSACTION) {
                    let linkedFullHashesLength = byteArray[pos];
                    pos++;
                    if (linkedFullHashesLength > 1) {
                        NRS.logConsole("currently only 1 full hash is supported");
                        return {fail: true, param: "PhasingAppendix", actual: linkedFullHashesLength, expected: 1};
                    }
                    if (linkedFullHashesLength == 1) {
                        let transactionId = params.phasingLinkedTransactions[0];
                        if (transactionId.chain != String(converters.byteArrayToSignedInt32(byteArray, pos))) {
                            return {
                                fail: true,
                                param: "PhasingAppendixChain",
                                actual: String(converters.byteArrayToSignedInt32(byteArray, pos)),
                                expected: transactionId.chain
                            };
                        }
                        pos += 4;
                        if (transactionId.transactionFullHash != converters.byteArrayToHexString(byteArray.slice(pos, pos + 32))) {
                            return {
                                fail: true,
                                param: "PhasingAppendixFullHash",
                                actual: converters.byteArrayToHexString(byteArray.slice(pos, pos + 32)),
                                expected: transactionId.transactionFullHash
                            };
                        }
                        pos += 32;
                    }
                }
                if (params.phasingVotingModel == NRS.constants.VOTING_MODELS.HASH) {
                    let hashedSecretLength = byteArray[pos];
                    pos++;
                    if (hashedSecretLength > 0 && converters.byteArrayToHexString(byteArray.slice(pos, pos + hashedSecretLength)) !== params.phasingHashedSecret) {
                        return {
                            fail: true,
                            param: "PhasingAppendixSecret",
                            actual: "",
                            expected: params.phasingHashedSecret
                        };
                    }
                    pos += hashedSecretLength;
                    let algorithm = String(byteArray[pos]);
                    if (algorithm !== "0" && algorithm != params.phasingHashedSecretAlgorithm) {
                        return {
                            fail: true,
                            param: "PhasingAppendixAlgorithm",
                            actual: algorithm,
                            expected: params.phasingHashedSecretAlgorithm
                        };
                    }
                }

                if (params.phasingVotingModel == NRS.constants.VOTING_MODELS.COMPOSITE) {
                    result = validateCompositePhasingData(byteArray, pos, params);
                    if (result.fail) {
                        return result;
                    } else {
                        pos = result.pos;
                    }
                }
            }
            return {pos: pos};
        };

        /**
         * Helper to adjust property names to match expected form data instead of the transaction field names.
         * Used when loading a voucher and calling the same verifyTransactionBytes that's used when validating
         * an answer from the server against the form data.
         *
         * @param requestType the requestType as string
         * @param transactionJSON the parsed transaction JSON (as an object)
         * @returns a copy of the transaction object with keys adjusted to match form data instead of transaction fields
         */
        NRS.buildDataFromTransactionJSON = function(requestType, transactionJSON) {
            let data = $.extend({}, transactionJSON);
            data = NRS.flattenObject(data, ["version."]);
            data.exchange = data.exchangeChain;
            switch(requestType) {
                case "setAlias":
                case "sellAlias":
                case "buyAlias":
                case "deleteAlias":
                    data.aliasName = data.alias;
                    delete data.alias;
                    data.aliasURI = data.uri;
                    delete data.uri;
                    break;
                case "createPoll":
                    if (Array.isArray(transactionJSON.attachment.options)) {
                        for(let i = 0; i < transactionJSON.attachment.options.length; i++) {
                            data['option' + (i < 10 ? '0' + i : i)] = transactionJSON.attachment.options[i];
                        }
                    }
                    break;
                case "dividendPayment":
                    data.height = transactionJSON.attachment.height;
                    break;
            }
            return data;
        }

        NRS.broadcastTransactionBytes = function (transactionBytes, callback, originalResponse, originalData, prunableAttachment) {
            let data = {
                "transactionBytes": transactionBytes,
                "prunableAttachmentJSON": JSON.stringify(prunableAttachment),
                "adminPassword": NRS.getAdminPassword()
            };
            let requestType = NRS.state && NRS.state.apiProxy ? "sendTransaction" : "broadcastTransaction";

            let fetchParams = {
                method: "POST",
                mode: "cors",
                cache: "no-cache",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            };
            const body = new URLSearchParams();
            for (const key in data) {
                body.append(key, data[key]);
            }
            fetchParams.body = body;
            fetch(NRS.getRequestPath() + "?requestType=" + requestType, fetchParams).then((response) => {
                if (response.ok) {
                    return response.text();
                }
                throw new TypeError(String(response.status));
            }).then((responseText) => {
                let response;
                try {
                    response = JSON.parse(String(responseText));
                } catch (e) {
                    let msg = "Failed to parse response JSON";
                    NRS.logConsole(msg + ":" + responseText);
                    response.errorCode = 1;
                    response.errorDescription = msg;
                    callback(response, originalData);
                    return;
                }
                NRS.escapeResponseObjStrings(response);
                if (NRS.console) {
                    NRS.addToConsole(this.url, this.type, this.data, response);
                }

                if (response.errorCode) {
                    if (!response.errorDescription) {
                        response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occurred.");
                    }
                    callback(response, originalData);
                } else if (response.error) {
                    response.errorCode = 1;
                    response.errorDescription = response.error;
                    callback(response, originalData);
                } else {
                    if ("transactionBytes" in originalResponse) {
                        delete originalResponse.transactionBytes;
                    }
                    originalResponse.broadcasted = true;
                    originalResponse.transaction = response.transaction;
                    originalResponse.fullHash = response.fullHash;
                    callback(originalResponse, originalData);
                    if (originalData.referencedTransactionFullHash) {
                        $.growl($.t("info_referenced_transaction_hash"), {
                            "type": "info"
                        });
                    }
                }
            }).catch((error) => {
                NRS.logConsole("request failed, error: " + error);
                if (error == "timeout") {
                    error = $.t("error_request_timeout");
                }
                callback({
                    "errorCode": -1,
                    "errorDescription": error
                }, {});
            });
        };

        NRS.generateQRCode = function (target, qrCodeData, minType, cellSize) {
            let type = minType ? minType : 2;
            while (type <= 40) {
                try {
                    let qr = qrcode(type, 'M');
                    qr.addData(qrCodeData);
                    qr.make();
                    let img = qr.createImgTag(cellSize);
                    NRS.logConsole("Encoded QR code of type " + type + " with cell size " + cellSize);
                    if (target) {
                        $(target).empty().append(img);
                    }
                    return img;
                } catch (e) {
                    type++;
                }
            }
            $(target).empty().html($.t("cannot_encode_message", qrCodeData.length));
        };

        function addAddressData(data) {
            if (typeof data == "object" && ("recipient" in data)) {
                let address = NRS.createRsAddress();
                if (NRS.isRsAccount(data.recipient)) {
                    data.recipientRS = data.recipient;
                    if (address.set(data.recipient)) {
                        data.recipient = address.account_id();
                    }
                } else {
                    if (address.set(data.recipient)) {
                        data.recipientRS = address.toString();
                    }
                }
            }
        }

        function addMissingData(data) {
            if (!("amountNQT" in data)) {
                data.amountNQT = "0";
            }
            if (!("recipient" in data)) {
                NRS.logConsole("No recipient in data");
            }
        }

        function validateControlPhasingData(data, byteArray, pos, hasFeeBurningData) {
            let params;
            let result;
            if (byteArray[pos] == 0xFF) {
                // Removal of account control
                if (data.controlVotingModel != NRS.constants.VOTING_MODELS.NONE) {
                    return {
                        fail: true,
                        param: "validateControlPhasingDataRemoval",
                        actual: data.controlVotingModel,
                        expected: NRS.constants.VOTING_MODELS.NONE
                    };
                }
                params = {phasingVotingModel: "-1", phasingQuorum: "0", phasingMinBalance: "0"}; // The server puts these bytes as control params so make sure they are there
                result = validateCommonPhasingData(byteArray, pos, params);
                if (result.fail) {
                    return result;
                } else {
                    pos = result.pos;
                }
            } else {
                params = JSON.parse(data["controlParams"]);
                result = validateCommonPhasingData(byteArray, pos, params);
                if (result.fail) {
                    return result;
                } else {
                    pos = result.pos;
                }
            }
            if (params.phasingVotingModel == NRS.constants.VOTING_MODELS.COMPOSITE) {
                result = validateCompositePhasingData(byteArray, pos, params);
                if (result.fail) {
                    return result;
                } else {
                    pos = result.pos;
                }
            }
            if (hasFeeBurningData) {
                let maxFeesSize = byteArray[pos];
                pos++;
                for (let i = 0; i < maxFeesSize; i++) {
                    // for now the client can only submit 0 or 1 control fees
                    // in case this is ever enhanced, we'll need to revisit this code
                    let controlMaxFees = String(converters.byteArrayToSignedInt32(byteArray, pos));
                    pos += 4;
                    controlMaxFees += ":" + String(converters.byteArrayToBigInteger(byteArray, pos));
                    pos += 8;
                    if (controlMaxFees != data.controlMaxFees) {
                        return {
                            fail: true,
                            param: "validateControlPhasingControlMaxFees",
                            actual: controlMaxFees,
                            expected: data.controlMaxFees
                        };
                    }
                }
                let minDuration = converters.byteArrayToSignedShort(byteArray, pos);
                pos += 2;
                if (data.controlMinDuration && minDuration != data.controlMinDuration || !data.controlMinDuration && minDuration != 0) {
                    return {
                        fail: true,
                        param: "validateControlPhasingControlMinDuration",
                        actual: minDuration,
                        expected: data.controlMinDuration
                    };
                }
                let maxDuration = converters.byteArrayToSignedShort(byteArray, pos);
                pos += 2;
                if (data.controlMaxDuration && maxDuration != data.controlMaxDuration || !data.controlMaxDuration && maxDuration != 0) {
                    return {
                        fail: true,
                        param: "validateControlPhasingControlMaxDuration",
                        actual: minDuration,
                        expected: data.controlMaxDuration
                    };
                }
            }
            return {pos: pos};
        }

        function validateCommonPhasingData(byteArray, pos, params) {
            let length;
            if (byteArray[pos] != (parseInt(params["phasingVotingModel"]) & 0xFF)) {
                return {
                    fail: true,
                    param: "validateCommonPhasingData",
                    actual: byteArray[pos],
                    expected: parseInt(params["phasingVotingModel"]) & 0xFF
                };
            }
            pos++;
            let quorum = String(converters.byteArrayToBigInteger(byteArray, pos));
            if (quorum !== "0" && quorum !== String(params["phasingQuorum"])) { // TODO improve this validation
                return {
                    fail: true,
                    param: "validateCommonPhasingDataQuorum",
                    actual: quorum,
                    expected: String(params["phasingQuorum"])
                };
            }
            pos += 8;
            let minBalance = String(converters.byteArrayToBigInteger(byteArray, pos));
            if (minBalance !== "0" && minBalance !== params["phasingMinBalance"]) { // TODO improve this validation
                return {
                    fail: true,
                    param: "validateCommonPhasingDataMinBalance",
                    actual: minBalance,
                    expected: params["phasingMinBalance"]
                };
            }
            pos += 8;
            let whiteListLength = byteArray[pos];
            pos++;
            for (let i = 0; i < whiteListLength; i++) {
                let accountId = converters.byteArrayToBigInteger(byteArray, pos);
                let accountRS = NRS.convertNumericToRSAccountFormat(accountId);
                pos += 8;
                if (String(accountId) !== params["phasingWhitelist"][i] && String(accountRS) !== params["phasingWhitelist"][i]) {
                    return {
                        fail: true,
                        param: "validateCommonPhasingDataAccount",
                        actual: accountId + " or " + accountRS,
                        expected: params["phasingWhitelist"][i]
                    };
                }
            }
            let holdingId = String(converters.byteArrayToBigInteger(byteArray, pos));
            if (holdingId !== "0" && holdingId !== params["phasingHolding"]) { // TODO improve this validation
                return {
                    fail: true,
                    param: "validateCommonPhasingDataHolding",
                    actual: holdingId,
                    expected: params["phasingHolding"]
                };
            }
            pos += 8;
            let minBalanceModel = String(byteArray[pos]);
            if (minBalanceModel !== "0" && minBalanceModel !== String(params["phasingMinBalanceModel"])) {
                return {
                    fail: true,
                    param: "validateCommonPhasingDataMinBalanceModel",
                    actual: minBalanceModel,
                    expected: params["phasingMinBalanceModel"]
                };
            }
            pos++;

            if (params.phasingVotingModel == NRS.constants.VOTING_MODELS.PROPERTY) {
                if (params.phasingSenderProperty === undefined) {
                    if (String(converters.byteArrayToBigInteger(byteArray, pos)) !== "0") {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataNoSenderProperty1",
                            actual: "",
                            expected: ""
                        };
                    }
                    pos += 8;
                    if (byteArray[pos] !== 0) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataNoSenderProperty2",
                            actual: "",
                            expected: ""
                        };
                    }
                    pos++;
                    if (byteArray[pos] !== 0) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataNoSenderProperty3",
                            actual: "",
                            expected: ""
                        };
                    }
                    pos++;
                } else {
                    if (String(converters.byteArrayToBigInteger(byteArray, pos)) !== params.phasingSenderProperty.setter) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataSenderProperty",
                            actual: String(converters.byteArrayToBigInteger(byteArray, pos)),
                            expected: params.phasingSenderProperty.setter
                        };
                    }
                    pos += 8;
                    length = byteArray[pos];
                    pos++;
                    if (converters.byteArrayToString(byteArray, pos, length) !== params.phasingSenderProperty.name) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataSenderPropertyName",
                            actual: converters.byteArrayToString(byteArray, pos, length),
                            expected: params.phasingSenderProperty.name
                        };
                    }
                    pos += length;
                    length = byteArray[pos];
                    pos++;
                    if (params.phasingSenderProperty.value === undefined) {
                        if (length !== 0) {
                            return {
                                fail: true,
                                param: "validateCommonPhasingDataSenderPropertyNoValue",
                                actual: "",
                                expected: ""
                            };
                        }
                    } else {
                        if (converters.byteArrayToString(byteArray, pos, length) !== params.phasingSenderProperty.value) {
                            return {
                                fail: true,
                                param: "validateCommonPhasingDataSenderPropertyValue",
                                actual: converters.byteArrayToString(byteArray, pos, length),
                                expected: params.phasingSenderProperty.value
                            };
                        }
                    }
                    pos += length;
                }
                if (params.phasingRecipientProperty === undefined) {
                    if (String(converters.byteArrayToBigInteger(byteArray, pos)) !== "0") {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataNoRecipientProperty1",
                            actual: "",
                            expected: ""
                        };
                    }
                    pos += 8;
                    if (byteArray[pos] !== 0) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataNoRecipientProperty2",
                            actual: "",
                            expected: ""
                        };
                    }
                    pos++;
                    if (byteArray[pos] !== 0) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataNoRecipientProperty3",
                            actual: "",
                            expected: ""
                        };
                    }
                    pos++;
                } else {
                    if (String(converters.byteArrayToBigInteger(byteArray, pos)) !== params.phasingRecipientProperty.setter) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataRecipientProperty",
                            actual: String(converters.byteArrayToBigInteger(byteArray, pos)),
                            expected: params.phasingRecipientProperty.setter
                        };
                    }
                    pos += 8;
                    length = byteArray[pos];
                    pos++;
                    if (converters.byteArrayToString(byteArray, pos, length) !== params.phasingRecipientProperty.name) {
                        return {
                            fail: true,
                            param: "validateCommonPhasingDataRecipientPropertyName",
                            actual: converters.byteArrayToString(byteArray, pos, length),
                            expected: params.phasingRecipientProperty.name
                        };
                    }
                    pos += length;
                    length = byteArray[pos];
                    pos++;
                    if (params.phasingRecipientProperty.value === undefined) {
                        if (length !== 0) {
                            return {
                                fail: true,
                                param: "validateCommonPhasingDataRecipientPropertyNoValue",
                                actual: "",
                                expected: ""
                            };
                        }
                    } else {
                        if (converters.byteArrayToString(byteArray, pos, length) !== params.phasingRecipientProperty.value) {
                            return {
                                fail: true,
                                param: "validateCommonPhasingDataRecipientPropertyValue",
                                actual: converters.byteArrayToString(byteArray, pos, length),
                                expected: params.phasingRecipientProperty.value
                            };
                        }
                    }
                    pos += length;
                }
            }

            return {pos: pos};
        }

        function validateCompositePhasingData(byteArray, pos, params) {
            let length = converters.byteArrayToSignedShort(byteArray, pos);
            pos += 2;
            let expression = converters.byteArrayToString(byteArray, pos, length);
            if (params.phasingExpression != expression) {
                return {
                    fail: true,
                    param: "validateCompositePhasingDataExpression",
                    actual: expression,
                    expected: params.phasingExpression
                };
            }
            pos += length;
            length = byteArray[pos];
            pos++;
            for (let i = 0; i < length; i++) {
                let subPollNameLength = byteArray[pos];
                pos++;
                let subPollName = converters.byteArrayToString(byteArray, pos, subPollNameLength);
                pos += subPollNameLength;
                if (!params.phasingSubPolls[subPollName]) {
                    return {
                        fail: true,
                        param: "validateCompositePhasingDataNoPollName",
                        actual: "",
                        expected: subPollName
                    };
                }
                let result = validateCommonPhasingData(byteArray, pos, params.phasingSubPolls[subPollName]);
                if (result.fail) {
                    return result;
                } else {
                    pos = result.pos;
                }
            }
            return {pos: pos};
        }

        return NRS;
    }(isNode ? client : NRS || {}, jQuery));

    if (isNode) {
        module.exports = NRS;
    }
});
