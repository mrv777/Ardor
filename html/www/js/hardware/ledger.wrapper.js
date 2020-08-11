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

let ledgerwallet = function() {
    const CLA = 0xe0;

    const P1_UNUSED = 0x00;
    const P1_INIT = 0x01;
    const P1_CONTINUE = 0x02;
    const P1_SIGN = 0x03;

    const P1_GET_PUBLIC_KEY = 0x01;
    const P1_GET_PUBLIC_KEY_CHAIN_CODE_AND_ED_PUBLIC_KEY = 0x02;

    const P1_TOKEN_INIT = 0x00;
    const P1_TOKEN_DATA = 0x01;
    const P1_TOKEN_SIGN = 0x02;

    const P1_ENCRYPT_SETUP = 0x01;
    const P1_ENCRYPT_DECRYPT = 0x04;

    const P2_UNUSED = 0x00;

    const CHUNK_SIZE = 32;

    const RET_VAL_PARSE_BUFFER_NEED_MORE_BYTES = 15;
    // const RET_VAL_KEY_DERIVE_EXCEPTION = 30;

    const INS = {
        GET_VERSION: 0x01,
        GET_PUBLIC_KEY: 0x02,
        CALC_TX_HASH: 0x03,
        ENCRYPT_DECRYPT_MSG: 0x04,
        SHOW_PUBLIC_KEY: 0x05,
        GET_PUBLIC_KEY_AND_CHAIN_CODE: 0x06,
        SIGN_TOKEN: 0x07
    };

    const ArdorResponseRCValues = {
        SUCCESS: 0,
        WRONG_RET_SIZE: 129,
        RET_WRONG_VERSION: 130,
        BAD_PARAM: 131,
        RET_WRONG_LENGTH: 133,
        RET_BUFFER_TOO_BIG: 134
    };

    const ErrorCodes = {
        ERR_STILL_IN_CALL: 0x6e04, // internal
        ERR_INVALID_DATA: 0x6e07,
        ERR_INVALID_BIP_PATH: 0x6e08,
        ERR_REJECTED_BY_USER: 0x6e09,
        ERR_REJECTED_BY_POLICY: 0x6e10,
        ERR_DEVICE_LOCKED: 0x6e11,

        // Not thrown by app itself but other apps
        ERR_CLA_NOT_SUPPORTED: 0x6e00
    };

    const ErrorMsgs = {
        [ErrorCodes.ERR_INVALID_DATA]: "Invalid data supplied to Ledger",
        [ErrorCodes.ERR_INVALID_BIP_PATH]: "Invalid derivation path supplied to Ledger",
        [ErrorCodes.ERR_REJECTED_BY_USER]: "Action rejected by user",
        [ErrorCodes.ERR_REJECTED_BY_POLICY]: "Action rejected by Ledger's security policy",
        [ErrorCodes.ERR_DEVICE_LOCKED]: "Device is locked",
        [ErrorCodes.ERR_CLA_NOT_SUPPORTED]: "Wrong Ledger app"
    };

    const getErrorDescription = (statusCode) => {
        const statusCodeHex = `0x${statusCode.toString(16)}`;
        const defaultMsg = `General error ${statusCodeHex}`;
        return ErrorMsgs[statusCode] || defaultMsg;
    };


    const ARDOR_SIG = [0xba, 0xbe, 0x00];

    const ArdorLedgerAppResponse = function ArdorLedgerAppResponse(rc, data) {
        this.rc = rc;
        this.data = data;
    };

    let Buffer = ledgerdevice.getNodeJsBufferClass();

    const callbackFunctions = [];

    const LEDGER_STATE = {
        DEVICE_READY: "DEVICE_READY",
        DEVICE_BUSY: "DEVICE_BUSY",
        DEVICE_NOT_CONNECTED_OR_LOCKED: "DEVICE_NOT_CONNECTED_OR_LOCKED",
        DEVICE_CONNECTED_APP_NOT_OPEN: "DEVICE_CONNECTED_APP_NOT_OPEN",
        DEVICE_LOCKED_IN_APP : "DEVICE_LOCKED_IN_APP",
        DEVICE_DISCONNECTED: "DEVICE_DISCONNECTED",
        DEVICE_VERSION_BELOW_MINIMUM_SUPPORTED: "DEVICE_VERSION_BELOW_MINIMUM_SUPPORTED",
        WEB_USB_NOT_SUPPORTED: "WEB_USB_NOT_SUPPORTED",
        DEVICE_NOT_SELECTED: "DEVICE_NOT_SELECTED",
        DEVICE_USED_BY_ANOTHER_SERVICE: "DEVICE_USED_BY_ANOTHER_SERVICE",
        DEVICE_INTERNAL_ERROR : "DEVICE_INTERNAL_ERROR"
    };

    const MIN_VERSION = [1, 6, 0];

    let version;
    let transport;
    let send;
    let sendThenStrip;

    async function connect() {
        if (transport) {
            return transport;
        }
        try {
            notifyCallbacks(LEDGER_STATE.DEVICE_BUSY);
            console.log("creating transport");
            transport = await connectWebUsb();
            if (transport === null) {
                return null;
            }
            let methods = [
                "getVersion",
                "getPublicKeys",
                "loadTxn",
                "signTxn"
            ];
            transport.decorateAppAPIMethods(this, methods, "ARD");
            transport.setDebugMode(true);
            send = wrapConvertError(transport.send);
            sendThenStrip = (inst, p1, p2, data) => send(CLA, inst, p1, p2, data).then(stripRetcodeFromResponse);
            if (version) {
                return;
            }

            try {
                version = await getVersion();
            } catch (error) {
                if ("DisconnectedDeviceDuringOperation" === error.name) {
                    notifyCallbacks(LEDGER_STATE.DEVICE_DISCONNECTED);
                    return null;
                } else {
                    console.log("getVersion error: " + error);
                    notifyCallbacks(LEDGER_STATE.DEVICE_INTERNAL_ERROR);
                    transport.close();
                    return null;
                }
            }

            if (0 !== version.rc) {
                console.log("Ret version returned", version.rc);
                notifyCallbacks(LEDGER_STATE.DEVICE_CONNECTED_APP_NOT_OPEN);
                transport.close();
                return null;
            }

            console.log("Ledger app version", JSON.stringify(version));

            if (!version.data.isSigCorrect) {
                console.log("is sig correct is wrong");
                notifyCallbacks(LEDGER_STATE.DEVICE_CONNECTED_APP_NOT_OPEN);
                transport.close();
                return null;
            }

            console.log("done");
            notifyCallbacks(LEDGER_STATE.DEVICE_READY);
            return transport;
        } catch (e) {
            console.log("unexpected error in getVersion", e);
            notifyCallbacks(e.message);
            return null;
        }
    }

    function onDisconnectWebUsb() {
        notifyCallbacks(LEDGER_STATE.DEVICE_DISCONNECTED);
        transport = version = null;
    }

    async function connectWebUsb() {
        let transport;
        try {
            console.log("connectWebUsb");
            if (!navigator.usb) {
                notifyCallbacks(LEDGER_STATE.WEB_USB_NOT_SUPPORTED);
                return null;
            }
            let devices = await navigator.usb.getDevices();
            console.log("connectWebUsb found usb devices", devices);
            let TransportWebUSB = ledgerdevice.getTransport();
            try {
                transport = await TransportWebUSB.openConnected();
                console.log("connectWebUsb openConnected transport", transport);
            } catch (e) {
                console.log("connectWebUsb openConnected transport failed (1)", e);
                notifyCallbacks(LEDGER_STATE.DEVICE_USED_BY_ANOTHER_SERVICE);
                transport = null;
            }
            if (null === transport) {
                console.log("connectWebUsb create transport");
                try {
                    transport = await TransportWebUSB.create();
                    console.log("connectWebUsb created transport", transport);
                } catch (e) {
                    console.log("connectWebUsb create transport failed (2)", e);
                    if (e.name === "TransportOpenUserCancelled") {
                        notifyCallbacks(LEDGER_STATE.DEVICE_NOT_SELECTED);
                    } else if (e.name === "TransportWebUSBGestureRequired") {
                        console.log("connectWebUsb gesture required");
                        notifyCallbacks(LEDGER_STATE.DEVICE_NOT_SELECTED);
                    }
                    return null;
                }
            }
            transport.on("disconnect", () => {
                onDisconnectWebUsb();
            });

            //if it's not a NanoS we don't sent Bolos commands to check the running app
            //because the Bolos protcol is different across NanoS and NanoX and
            //future devices to come

            if ("nanoS" !== transport.deviceModel.id) {
                notifyCallbacks(LEDGER_STATE.DEVICE_READY);
                return transport;
            }

            const appMetadata = await transport.send(0xb0, 0x01, 0x00, 0x00);
            console.log("app metadata", appMetadata);
            let i = 0;
            const format = appMetadata[i++];
            if (1 !== format) {
                console.log("Format is not as expected, result:", appMetadata);
                notifyCallbacks(LEDGER_STATE.DEVICE_INTERNAL_ERROR);
                await transport.close();
                return null;
            }
            const nameLength = appMetadata[i++];
            const name = appMetadata.slice(i, (i += nameLength)).toString("ascii");
            const versionLength = appMetadata[i++];
            const version = appMetadata.slice(i, (i += versionLength)).toString("ascii");
            const flagLength = appMetadata[i++];
            const flags = appMetadata.slice(i, flagLength);
            console.log("Ledger app info", name, version, flags);
            if ("BOLOS" === name) {
                let versionParts = version.split('.');
                if (MIN_VERSION.length !== versionParts.length) {
                    console.log("Bad version length", versionParts);
                    notifyCallbacks(LEDGER_STATE.DEVICE_INTERNAL_ERROR);
                    await transport.close();
                    return null;
                }

                for (let i = 0; i < MIN_VERSION.length; i++) {
                    if (parseInt(versionParts[i]) < MIN_VERSION[i]) {
                        console.log("Version too low", versionParts);
                        notifyCallbacks(LEDGER_STATE.DEVICE_VERSION_BELOW_MINIMUM_SUPPORTED);
                        await transport.close();
                        return null;
                    }
                }
                notifyCallbacks(LEDGER_STATE.DEVICE_CONNECTED_APP_NOT_OPEN);
                await transport.close();
                return null;
            } else if ("Ardor" !== name) {
                console.log("App is not ardor its ", name);
                notifyCallbacks(LEDGER_STATE.DEVICE_CONNECTED_APP_NOT_OPEN);
                await transport.close();
                return null;
            }
            return transport;
        } catch (error) {
            switch (error.name) {
                case "TransportOpenUserCancelled":
                    console.log("TransportOpenUserCancelled");
                    notifyCallbacks(LEDGER_STATE.DEVICE_NOT_CONNECTED_OR_LOCKED);
                    break;
                case "TransportInterfaceNotAvailable":
                    console.log("TransportInterfaceNotAvailable");
                    notifyCallbacks(LEDGER_STATE.DEVICE_CONNECTED_APP_NOT_OPEN);
                    break;
                case "TransportStatusError":
                    console.log("TransportStatusError");
                    notifyCallbacks(LEDGER_STATE.DEVICE_CONNECTED_APP_NOT_OPEN);
                    break;
                default:
                    console.log("Unexpected error", error);
                    notifyCallbacks(LEDGER_STATE.DEVICE_INTERNAL_ERROR);
            }
            if (transport) {
                transport.close();
            }
            return null;
        }
    }

    function stripRetcodeFromResponse(response) {
        if (!Buffer.isBuffer(response)) {
            throw new Error("Response is not a buffer");
        }
        if (!(response.length >= 2)) {
            throw new Error("Response must contain at least 2 bytes");
        }

        const index = response.length - 2;
        const retcode = response.slice(index, index + 2);

        if (retcode.toString("hex") !== "9000") {
            throw new Error(`Invalid retcode ${retcode.toString("hex")}`);
        }
        return response.slice(0, index);
    }

    // It can happen that we try to send a message to the device
    // when the device thinks it is still in a middle of previous ADPU stream.
    // This happens mostly if host does abort communication for some reason
    // leaving ledger mid-call.
    // In this case Ledger will respond by ERR_STILL_IN_CALL *and* resetting its state to
    // default. We can therefore transparently retry the request.
    // Note though that only the *first* request in an multi-APDU exchange should be retried.
    const wrapRetryStillInCall = fn => async (...args) => {
        try {
            return await fn(...args);
        } catch (e) {
            if (e && e.statusCode && e.statusCode === ErrorCodes.ERR_STILL_IN_CALL) {
                console.log("retry ledger command: " + fn);
                return await fn(...args);
            }
            throw e;
        }
    };

    const wrapConvertError = fn => async (...args) => {
        try {
            return await fn(...args);
        } catch (e) {
            if (e && e.statusCode) {
                // keep HwTransport.TransportStatusError just override the message
                e.message = `Ledger device: ${getErrorDescription(e.statusCode)}`;
            }
            throw e;
        }
    };

    function notifyCallbacks(msg) {
        console.log("setState to " + msg);
        callbackFunctions.forEach(function (callbackFunction) {
            callbackFunction(msg);
        });
    }

    function registerCallBack(func) {
        callbackFunctions.push(func);
    }

    async function getVersion() {
        const P1_UNUSED = 0x00;
        const P2_UNUSED = 0x00;
        const response = await wrapRetryStillInCall(sendThenStrip)(INS.GET_VERSION, P1_UNUSED, P2_UNUSED, Buffer.alloc(0));
        if (7 !== response.length) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.WRONG_RET_SIZE, response);
        }
        const appVersionMajor = response[0];
        const appVersionMinor = response[1];
        const appVersionPatch = response[2];
        const flags = response[3];
        const sig1 = response[4];
        const sig2 = response[5];
        const sig3 = response[6];
        const isSigCorrect = ARDOR_SIG[0] === sig1 && ARDOR_SIG[1] === sig2 && ARDOR_SIG[2] === sig3;
        return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, {
            version: `${appVersionMajor}.${appVersionMinor}.${appVersionPatch}`,
            isSigCorrect: isSigCorrect,
            flags: {
                isDebug: 0 !== flags
            }
        });
    }

    async function getPublicKeyAndChainCode(path, isGetPublicKeyAndChainCode) {
        const pathData = parsePath(path)
        if (!pathData.isValid) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }

        const _send = (p1, p2, data) => send(CLA, INS.GET_PUBLIC_KEY_AND_CHAIN_CODE, p1, p2, data).then(stripRetcodeFromResponse);

        let P1 = P1_GET_PUBLIC_KEY;
        let expectedSize = 1 + 32;

        if (isGetPublicKeyAndChainCode) {
            P1 = P1_GET_PUBLIC_KEY_CHAIN_CODE_AND_ED_PUBLIC_KEY;
            expectedSize += 32 + 32;
        }

        const result = await wrapRetryStillInCall(_send) (P1, P2_UNUSED, pathData.bytes);

        if (result.length === 0) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
        }

        if (result[0] !== 0) {
            return new ArdorLedgerAppResponse(result[0]);
        }

        if (result.length !== expectedSize) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
        }

        let edPublicKey = null;
        let chainCode = null;
        if (isGetPublicKeyAndChainCode) {
            edPublicKey = result.slice(1 + 32, 1 + 32 + 32);
            chainCode =   result.slice(1 + 32 + 32, 1 + 32 + 32 + 32);
        }

        return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, {
            chainCode: chainCode,
            publicKeyEd25519: edPublicKey,
            publicKeyCurve: result.slice(1, 33)
        });
    }

    function chunkedBase32BufferEncode(msg) {
        let ret = "";
        for (let ptr = 0; ptr < 100; ptr += 5) {
            let value = 0; //dont need biginteger here, Number's range can handle 2^(5*8)
            for (let i = 4; i >= 0; i--) {
                value = value * 256 + (msg[ptr + i] & 0xFF);
            }
            ret += "0".repeat(8 - Math.ceil(Math.log(value) / Math.log(32)));
            ret += value.toString(32);
            if ((ret.length % 8) !== 0) {
                console.log("error " + value + " " + ret + " " + ptr )
            }
        }
        return ret;
    }

    async function signToken(path, timestamp, message) {
        const pathData = parsePath(path);
        if (!pathData.isValid) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }
        if ((timestamp < 0) || (timestamp > 0xffffffff)) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }
        const timeStampBuffer = Buffer.alloc(4);
        timeStampBuffer.writeUInt32LE(timestamp, 0);

        let messageBuffer = Buffer.alloc(message.length);
        for (let i = 0; i < message.length; i++) {
            if (message[i] < 0 || message[i] > 255) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
            }
            messageBuffer.writeUInt8(message[i], i);
        }

        const _send = (p1, p2, data) => send(CLA, INS.SIGN_TOKEN, p1, p2, data).then(stripRetcodeFromResponse);
        let result = await wrapRetryStillInCall(_send) (P1_TOKEN_INIT, P2_UNUSED, Buffer.alloc(0));

        if (result.length !== 1) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
        }

        if (result[0] !== 0) {
            return new ArdorLedgerAppResponse(result[0]);
        }

        let pos = 0;
        while (pos < messageBuffer.length) {
            let endPos = Math.min(messageBuffer.length, pos + 240);
            result = await wrapRetryStillInCall(_send)(P1_TOKEN_DATA, P2_UNUSED, messageBuffer.slice(pos, endPos));
            if (result.length !== 1) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
            }
            if (result[0] !== 0) {
                return new ArdorLedgerAppResponse(result[0], result);
            }
            pos = endPos;
        }
        result = await wrapRetryStillInCall(_send)(P1_TOKEN_SIGN, P2_UNUSED, Buffer.concat([timeStampBuffer, pathData.bytes]));
        if (result.length === 101 && result[0] === 0) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, chunkedBase32BufferEncode(result.slice(1, 101)));
        }
        return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
    }

    async function showAddress(path) {
        const pathData = parsePath(path);
        if (!pathData.isValid) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }
        const _send = (p1, p2, data) => send(CLA, INS.SHOW_PUBLIC_KEY, p1, p2, data).then(stripRetcodeFromResponse);
        const result = await wrapRetryStillInCall(_send) (P1_UNUSED, P2_UNUSED, pathData.bytes);

        if (result.length !== 1) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
        }

        if (result[0] !== 0) {
            return new ArdorLedgerAppResponse(result[0]);
        }

        return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS);
    }


    async function decryptBuffer(path, targetPublicKey, nonce, IV, encryptedData, returnSharedKey) {
        if (targetPublicKey.length !== 32 || (encryptedData.length % 16) !== 0 || nonce.length !== 32 || IV.length !== 16) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }
        const pathData = parsePath(path);
        if (!pathData.isValid) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }

        const firstBufferToSend = Buffer.concat([pathData.bytes, Buffer.from(targetPublicKey), Buffer.from(nonce), Buffer.from(IV)]);
        const _send = (p1, p2, data) => send(CLA, INS.ENCRYPT_DECRYPT_MSG, p1, p2, data).then(stripRetcodeFromResponse);

        let P1;
        if (returnSharedKey) {
            P1 = 0x03;
        } else {
            P1 = 0x02;
        }
        let result = await wrapRetryStillInCall(_send) (P1, P2_UNUSED, firstBufferToSend);
        if (returnSharedKey && result.length !== 33 || !returnSharedKey && result.length !== 1) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
        }

        if (result[0] !== 0) {
            return new ArdorLedgerAppResponse(result[0], result);
        }

        let sharedKey = null;
        if (returnSharedKey) {
            if (result.length < 33) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
            }
            sharedKey = result.slice(1, 33);
        }

        let encryptedBuffer = Buffer.from(encryptedData);
        let pos = 0;
        let unencryptedData = Buffer.alloc(0);

        while (pos < encryptedBuffer.length) {
            let endPos = Math.min(encryptedBuffer.length, pos + 240);
            result = await wrapRetryStillInCall(_send)(P1_ENCRYPT_DECRYPT, P2_UNUSED, encryptedBuffer.slice(pos, endPos));
            if (result.length !== endPos - pos + 1) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
            }
            if (result[0] !== 0) {
                return new ArdorLedgerAppResponse(result[0], result);
            }
            unencryptedData = Buffer.concat([unencryptedData, result.slice(1, result.length)]);
            pos = endPos;
        }
        return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, {
            resultBuffer: unencryptedData,
            sharedKey: sharedKey
        });
    }

    async function encryptBuffer(path, targetPublicKey, data) {
        if (targetPublicKey.length !== 32 || (data.length % 16) !== 0) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }
        const pathData = parsePath(path);
        if (!pathData.isValid) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }

        const _send = (p1, p2, data) => send(CLA, INS.ENCRYPT_DECRYPT_MSG, p1, p2, data).then(stripRetcodeFromResponse);
        let result = await wrapRetryStillInCall(_send) (
            P1_ENCRYPT_SETUP,
            P2_UNUSED,
            Buffer.concat([pathData.bytes, Buffer.from(targetPublicKey)])
        );

        if (result.length !== 1 + 32 + 16) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
        }
        if (result[0] !== 0) {
            return new ArdorLedgerAppResponse(result[0], result);
        }

        let nonce = result.slice(1, 1 + 32);
        let IV = result.slice(1 + 32, 1 + 32 + 16);
        let pos = 0;
        let dataBuffer = Buffer.from(data);
        let retData = Buffer.alloc(0);

        while (pos < dataBuffer.length) {
            let sendLength = Math.min(240, dataBuffer.length - pos);
            result = await wrapRetryStillInCall(_send)(P1_ENCRYPT_DECRYPT, P2_UNUSED, dataBuffer.slice(pos, pos + sendLength));
            pos += sendLength;

            if (result.length !== sendLength + 1) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
            }

            if (0 !== result[0]) {
                return new ArdorLedgerAppResponse(result[0], result);
            }
            retData = Buffer.concat([retData, result.slice(1, 1 + sendLength)]);
        }

        return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, {
            nonce: nonce,
            IV: IV,
            encryptedData: retData
        });
    }

    async function loadTxn(txnArray) {
        let txnBuffer = Buffer.from(txnArray);
        const _send = (p1, p2, data) => send(CLA, INS.CALC_TX_HASH, p1, p2, data).then(stripRetcodeFromResponse);

        if ((txnBuffer.length & 0b1100000000000000) > 0) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_BUFFER_TOO_BIG);
        }

        let P1 = ((txnBuffer.length & 0b0011111100000000) >> 6) | P1_INIT;
        let P2 = txnBuffer.length & 0xff;
        let pos = 0;
        while (pos < txnBuffer.length) {
            let endPos = Math.min(txnBuffer.length, pos + CHUNK_SIZE);
            const result = await wrapRetryStillInCall(_send)(P1, P2, txnBuffer.slice(pos, endPos));
            pos = endPos;
            P1 = P1_CONTINUE;
            P2 = 0;

            if (endPos === txnBuffer.length) { //Are all the bytes sent?
                if (result.length < 2) {
                    return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
                }

                if (result[1] === 1) { //RET_VAL_REJECT_BUTTON todo maybe this value will change some day, find a way to sync these
                    return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, false);
                }

                if (result[1] === 8) { //RET_VAL_PARSE_BUFFER_FINISHED todo maybe this value will change some day, find a way to sync these
                    return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, true);
                }
                return new ArdorLedgerAppResponse(result[1]);
            }
            if (result.length < 1) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
            }
            if (result[0] !== 0) {
                return new ArdorLedgerAppResponse(result[0]);
            }
            if (result.length < 2) {
                return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
            }
            if (result[1] !== RET_VAL_PARSE_BUFFER_NEED_MORE_BYTES) {  //RET_VAL_REJECT_BUTTON todo maybe this value will change some day, find a way to sync these
                return new ArdorLedgerAppResponse(result[1]);
            }
        }
    }

    async function signTxn(path) {
        let pathData = parsePath(path);
        if (!pathData.isValid) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.BAD_PARAM);
        }
        const _send = (p1, p2, data) => send(CLA, INS.CALC_TX_HASH, p1, p2, data).then(stripRetcodeFromResponse);
        const result = await wrapRetryStillInCall(_send)(P1_SIGN, P2_UNUSED, pathData.bytes);
        if (result.length === 64 + 1 && result[0] === 0) {
            return new ArdorLedgerAppResponse(ArdorResponseRCValues.SUCCESS, result.slice(1, 64 + 1));
        }
        return new ArdorLedgerAppResponse(ArdorResponseRCValues.RET_WRONG_LENGTH, result);
    }

    function parsePath(path) {
        if (path.length < 3 || path.length > 10) {
            return { isValid: false };
        }
        let bytes = Buffer.alloc(4 * path.length);
        for (let i = 0; i < path.length; i++) {
            bytes.writeUInt32LE(path[i], i * 4);
        }
        return { isValid: true, bytes: bytes }
    }

    return {
        connect: connect,
        registerCallBack: registerCallBack,
        getPublicKeyAndChainCode: getPublicKeyAndChainCode,
        loadTxn: loadTxn,
        signTxn: signTxn,
        signToken: signToken,
        showAddress: showAddress,
        encryptBuffer: encryptBuffer,
        decryptBuffer: decryptBuffer
    }
}();