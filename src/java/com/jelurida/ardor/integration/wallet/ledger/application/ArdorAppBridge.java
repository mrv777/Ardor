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

package com.jelurida.ardor.integration.wallet.ledger.application;

import com.jelurida.ardor.integration.wallet.ledger.LedgerDevice;
import nxt.Constants;
import nxt.account.Token;
import nxt.crypto.DecryptedData;
import nxt.crypto.EncryptedData;
import nxt.util.Bip32Path;
import nxt.util.Convert;
import nxt.util.Logger;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static nxt.util.Convert.EMPTY_BYTE;

public class ArdorAppBridge implements ArdorAppInterface {

    private static final int RET_VAL_TRANSACTION_REJECTED = 1;
    private static final int RET_VAL_TRANSACTION_ACCEPTED = 8;
    private static final int RET_VAL_PARSE_BUFFER_NEED_MORE_BYTES = 15;
    private static final int RET_VAL_KEY_DERIVE_EXCEPTION = 30;

    private static final int P1_INIT = 0x01;
    private static final int P1_CONTINUE = 0x02;
    private static final int P1_SIGN = 0x03;

    private static final int P1_ENCRYPTION_SETUP = 0x01;
    @SuppressWarnings("unused")
    private static final int P1_ENCRYPTION_DECRYPT_NO_SHARED_KEY = 0x02;
    private static final int P1_ENCRYPTION_DECRYPT_AND_RETURN_SHARED_KEY = 0x03;
    private static final int P1_ENCRYPTION_PROCESS_DATA = 0x04;

    private static final int P1_GET_PUBLIC_KEY = 0x01;
    private static final int P1_GET_PUBLIC_KEY_CHAIN_CODE_AND_ED_PUBLIC_KEY = 0x02;

    private static final int CHUNK_SIZE = 128;

    private static final byte[] ARDOR_SIG = new byte[]{(byte) (0xba & 0xff), (byte) (0xbe & 0xff), 0x00};
    private static final int APP_PREFIX = 0xe0;

    private static final int SIGNATURE_LENGTH = 64;

    private static final ArdorAppBridge app = new ArdorAppBridge(Constants.LEDGER_WALLET_ENABLED);

    private final boolean isEnabled;
    private volatile LedgerDevice ledgerDevice;
    private volatile String lastError = "";

    public static ArdorAppInterface getApp() {
        return app;
    }

    enum INSTRUCTIONS {
        GET_VERSION(0x01),
        GET_PUBLIC_KEY(0x02),
        CALC_TX_HASH(0x03),
        ENCRYPT_DECRYPT_MSG(0x04),
        SHOW_PUBLIC_KEY(0x05),
        GET_PUBLIC_KEY_AND_CHAIN_CODE(0x06),
        SIGN_TOKEN(0x07);

        int code;

        INSTRUCTIONS(int code) {
            this.code = code;
        }
    }

    private ArdorAppBridge(boolean ledgerWalletEnabled) {
        isEnabled = ledgerWalletEnabled;
    }

    private synchronized boolean checkConnection() {
        return checkConnection(true);
    }

    private synchronized boolean checkConnection(boolean retryExchange) {
        if (!isEnabled) {
            ledgerDevice = null;
            lastError = AppReturnCode.DEVICE_NOT_ENABLED_IN_NODE_CONFIGURATION.name();
            return false;
        }
        if (ledgerDevice != null) {
            return true;
        }
        ledgerDevice = LedgerDevice.findLedgerDevice();
        if (ledgerDevice == null) {
            Logger.logInfoMessage("hardware wallet is disconnected or locked");
            lastError = AppReturnCode.DEVICE_NOT_CONNECTED_OR_LOCKED.name();
            return false;
        }
        if (isInvalidAppSignature(retryExchange)) {
            Logger.logInfoMessage("hardware wallet signature is invalid");
            lastError = AppReturnCode.DEVICE_CONNECTED_APP_NOT_OPEN.name();
            ledgerDevice = null;
            return false;
        }
        Logger.logInfoMessage("hardware wallet connection established");
        return true;
    }

    private static byte[] generateBuffer(INSTRUCTIONS instruction, int p1, byte[] data) {
        return generateBuffer(instruction, p1, 0, data);
    }

    private static byte[] generateBuffer(INSTRUCTIONS instruction, int p1, int p2, byte[] data) {
        int size = 4;
        if (data != null) {
            size += 1 + data.length;
        }
        ByteBuffer b = ByteBuffer.allocate(size);
        b.put((byte) APP_PREFIX);
        b.put((byte) instruction.code);
        b.put((byte) p1);
        b.put((byte) p2);
        if (data != null) {
            b.put((byte) (data.length));
            b.put(data);
        }
        return b.array();
    }

    private byte[] exchange(byte[] data) {
        return exchange(data, true);
    }

    private byte[] exchange(byte[] data, boolean retry) {
        clearLastError();
        try {
            byte[] response = ledgerDevice.exchange(data);
            if (response != null) {
                return response;
            }
        } catch (Throwable t) {
            Logger.logInfoMessage("Device error", t);
        }

        try {
            if (retry) {
                Logger.logDebugMessage("Resetting ArdorAppBridge and retrying exchange.");
                ledgerDevice = null;
                if (checkConnection(false)) {
                    return exchange(data, false);
                }
                return EMPTY_BYTE;
            }
        } catch (Throwable t) {
            Logger.logInfoMessage("Device error on retry", t);
        }

        setLastError(new ArdorAppException(AppReturnCode.DEVICE_NOT_CONNECTED_OR_LOCKED));
        return EMPTY_BYTE;
    }

    // not private for testing
    boolean isInvalidAppSignature() {
        return isInvalidAppSignature(true);
    }

    private boolean isInvalidAppSignature(boolean retryExchange) {
        byte[] data = generateBuffer(INSTRUCTIONS.GET_VERSION, 0, null);
        byte[] result = exchange(data, retryExchange);
        if (result.length == 0) {
            return true;
        }
        if (result.length != 7) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_APP_VERSION, result));
            return true;
        }
        boolean isSigCorrect = (ARDOR_SIG[0] == result[4]) && (ARDOR_SIG[1] == result[5]) && (ARDOR_SIG[2] == result[6]);
        Logger.logInfoMessage("Ledger app version %d.%d.%d (flags 0x%x), isSigCorrect %b",
                result[0], result[1], result[2], result[3], isSigCorrect);
        return !isSigCorrect;
    }

    private boolean isInvalidResult(byte[] result) {
        if (result == null) {
            setLastError(new ArdorAppException(AppReturnCode.GENERAL_FAILURE, null));
            return true;
        }
        if (0 == result.length) {
            if (getLastError() == null) {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
            }
            return true;
        }
        if (0 != result[0]) {
            if (result[0] == 0x0b) {
                setLastError(new ArdorAppException(AppReturnCode.DEVICE_LOCKED_IN_APP, result));
            } else {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_VALUE, result));
            }
            return true;
        }
        return false;
    }

    /**
     * Retrieves the public key given the <b>offset</b> from the HD path
     *
     * @param pathStr of the given address
     * @param isGetMasterKey also get the master public key and chain code
     * @return the public key
     */
    public byte[] getWalletPublicKeys(String pathStr, boolean isGetMasterKey) {
        if (!checkConnection()) {
            return EMPTY_BYTE;
        }
        int[] bip32Path = Bip32Path.bip32StrToPath(pathStr);
        PublicKeyData publicKeyData = getPublicKeyData(bip32Path, isGetMasterKey);
        if (isGetMasterKey) {
            return publicKeyData.getData();
        } else {
            return publicKeyData.getCurve25519PublicKey();
        }
    }

    /**
     * Given a parent bip32 path - return the master public key for this path.
     * We can use this master public key to generate any number of child public keys without a connected ledger device.
     * This public key is an ed25519 key so cannot be used as a normal public key.
     *
     * @param pathStr of the given address
     * @return the public key and chain code
     */
    @Override
    public PublicKeyData getPublicKeyData(String pathStr) {
        if (!checkConnection()) {
            return null;
        }
        int[] bip32Path = Bip32Path.bip32StrToPath(pathStr);
        return getPublicKeyData(bip32Path, true);
    }

    @Override
    public PublicKeyData getPublicKeyData(int[] bip32Path, boolean isGetPublicKeyAndChainCode) {
        if (!checkConnection()) {
            return new PublicKeyData(null);
        }
        int flags = P1_GET_PUBLIC_KEY;
        int expectedSize = 1 + 32;
        if (isGetPublicKeyAndChainCode) {
            flags = P1_GET_PUBLIC_KEY_CHAIN_CODE_AND_ED_PUBLIC_KEY;
            expectedSize += 32 + 32;
        }

        byte[] data = generateBuffer(INSTRUCTIONS.GET_PUBLIC_KEY_AND_CHAIN_CODE, flags, Bip32Path.bip32PathToBytes(bip32Path));
        byte[] result = exchange(data);
        if (isInvalidResult(result)) {
            return new PublicKeyData(null);
        }
        if (result.length != expectedSize) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
            return new PublicKeyData(null);
        }
        return new PublicKeyData(result);
    }

    public boolean loadWalletTransaction(String buffer) {
        if (!checkConnection()) {
            return false;
        }
        return loadImpl(Convert.parseHexString(buffer));
    }

    private boolean loadImpl(byte[] buffer) {
        Logger.logInfoMessage("loadWalletTransaction buffer of length %d with data %s", buffer.length ,Arrays.toString(buffer));
        // Encode the length into the p1 and p2 parameters
        if (0 != (buffer.length & 0b1100000000000000)) {
            setLastError(new ArdorAppException(AppReturnCode.RET_BUFFER_TOO_BIG));
            return false;
        }
        int p1 = ((buffer.length & 0b0011111100000000) >> 6) | P1_INIT;
        int p2 = buffer.length & 0xFF;
        for (int i = 0; i < buffer.length; i += CHUNK_SIZE) {
            int len = Math.min(buffer.length - i, CHUNK_SIZE);
            byte[] dataChunk = new byte[len];
            System.arraycopy(buffer, i, dataChunk, 0, len);
            byte[] data = generateBuffer(INSTRUCTIONS.CALC_TX_HASH, p1, p2, dataChunk);
            byte[] result = exchange(data);
            if (result.length == 0) {
                return false;
            }
            if (result[0] == 0x0b) {
                setLastError(new ArdorAppException(AppReturnCode.DEVICE_LOCKED_IN_APP, result));
                return false;
            }
            p1 = P1_CONTINUE;
            p2 = 0;
            if (i + CHUNK_SIZE > buffer.length) { //Are all the bytes sent?
                if (result.length < 2) {
                    setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
                    return false;
                }
                if (result[1] == RET_VAL_TRANSACTION_REJECTED) {
                    return false;
                }
                if (result[1] == RET_VAL_TRANSACTION_ACCEPTED) {
                    return true;
                }
                setLastError(new ArdorAppException(result[1], result));
                return false;
            }
            if (result[0] != 0) {
                setLastError(new ArdorAppException(result[0], result));
                return false;
            }
            if (result.length < 2) {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
                return false;
            }
            if (result[1] != RET_VAL_PARSE_BUFFER_NEED_MORE_BYTES) {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
                return false;
            }
        }
        setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, buffer));
        return false;
    }

    public byte[] signWalletTransaction(String pathStr) {
        if (!checkConnection()) {
            return EMPTY_BYTE;
        }
        int[] path = Bip32Path.bip32StrToPath(pathStr);
        return signTransactionImpl(path);
    }

    private byte[] signTransactionImpl(int[] path) {
        byte[] data = generateBuffer(INSTRUCTIONS.CALC_TX_HASH, P1_SIGN, Bip32Path.bip32PathToBytes(path));
        byte[] result = exchange(data);
        if (isInvalidResult(result)) {
            if (RET_VAL_KEY_DERIVE_EXCEPTION == result[0]) {
                if (result.length < 3) {
                    setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH.getCode(), result));
                } else {
                    setLastError(new ArdorAppException(RET_VAL_KEY_DERIVE_EXCEPTION, result));
                }
            }
            return EMPTY_BYTE;
        }

        if (1 + SIGNATURE_LENGTH != result.length) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH.getCode(), result));
            return EMPTY_BYTE;
        }
        byte[] signature = new byte[SIGNATURE_LENGTH];
        System.arraycopy(result, 1, signature, 0, SIGNATURE_LENGTH);
        return signature;
    }

    public String signToken(String pathStr, int timestamp, String messageHex) {
        if (!checkConnection()) {
            return null;
        }
        int[] path = Bip32Path.bip32StrToPath(pathStr);
        return signTokenImpl(path, timestamp, messageHex);
    }

    @SuppressWarnings("ConstantConditions")
    private String signTokenImpl(int[] path, int timestamp, String messageHex) {
        byte[] data = generateBuffer(INSTRUCTIONS.SIGN_TOKEN, 0, null);
        byte[] result = exchange(data);
        if (isInvalidResult(result)) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH.getCode(), result));
            return "";
        }
        int pos = 0;
        byte[] messageBuffer = Convert.parseHexString(messageHex);
        while (pos < messageBuffer.length) {
            int endPos = Math.min(messageBuffer.length, pos + 240);
            byte[] buffer = Arrays.copyOfRange(messageBuffer, pos, endPos);
            data = generateBuffer(INSTRUCTIONS.SIGN_TOKEN, 1, buffer);
            result = exchange(data);
            if (isInvalidResult(result)) {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH.getCode(), result));
                return "";
            }
            pos = endPos;
        }
        byte[] timestampBytes = Convert.intToLittleEndian(timestamp);
        byte[] pathBytes = Bip32Path.bip32PathToBytes(path);
        byte[] buffer = new byte[timestampBytes.length + pathBytes.length];
        System.arraycopy(timestampBytes, 0, buffer, 0, timestampBytes.length);
        System.arraycopy(pathBytes, 0, buffer, timestampBytes.length, pathBytes.length);
        data = generateBuffer(INSTRUCTIONS.SIGN_TOKEN, 2, buffer);
        result = exchange(data);
        if (isInvalidResult(result)) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH.getCode(), result));
            return "";
        }
        if (result.length != 101) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH.getCode(), result));
            return "";
        }
        return Token.encodeToken(Arrays.copyOfRange(result, 1, 101));
    }

    @Override
    public String encryptBuffer(String pathStr, String publicKeyHex, String messageBytesHex) {
        if (!checkConnection()) {
            return null;
        }
        int[] path = Bip32Path.bip32StrToPath(pathStr);
        byte[] recipientPublicKey = Convert.parseHexString(publicKeyHex);
        byte[] messageBytes = Convert.parseHexString(messageBytesHex);
        EncryptedData encryptedData = encryptBuffer(path, recipientPublicKey, messageBytes);
        if (encryptedData == null) {
            return null;
        }
        return Convert.toHexString(encryptedData.getData()) + "," + Convert.toHexString(encryptedData.getNonce());
    }

    // not private for unit tests
    EncryptedData encryptBuffer(int[] path, byte[] targetPublicKey, byte[] data) {
        if (32 != targetPublicKey.length || 2 > path.length || 32 < path.length) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, data));
            return null;
        }

        byte[] bip32Path = Bip32Path.bip32PathToBytes(path);
        data = Convert.pkcs7Pad(Convert.compress(data));

        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        bytesStream.write(bip32Path, 0, bip32Path.length);
        bytesStream.write(targetPublicKey, 0, targetPublicKey.length);
        byte[] buffer = bytesStream.toByteArray();
        byte[] result = exchange(generateBuffer(INSTRUCTIONS.ENCRYPT_DECRYPT_MSG, P1_ENCRYPTION_SETUP, buffer));
        if (isInvalidResult(result)) {
            return null;
        }
        byte[] nonce = new byte[32];
        byte[] iv = new byte[16];
        if (1 + nonce.length + iv.length != result.length) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
            return null;
        }
        System.arraycopy(result, 1, nonce, 0, nonce.length);
        System.arraycopy(result, 1 + 32, iv, 0, iv.length);

        int pos = 0;
        bytesStream = new ByteArrayOutputStream();
        bytesStream.write(iv, 0, iv.length);

        while (pos < data.length) {
            int sendLength = Math.min(CHUNK_SIZE, data.length - pos);
            byte[] chunk = new byte[sendLength];
            System.arraycopy(data, pos, chunk, 0, sendLength);
            result = exchange(generateBuffer(INSTRUCTIONS.ENCRYPT_DECRYPT_MSG, P1_ENCRYPTION_PROCESS_DATA, chunk));
            pos += sendLength;
            if (isInvalidResult(result)) {
                return null;
            }
            if (result.length != sendLength + 1) {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
                return null;
            }
            bytesStream.write(result, 1, sendLength);
        }
        return new EncryptedData(bytesStream.toByteArray(), nonce);
    }

    @Override
    public String decryptBuffer(String pathStr, String publicKeyHex, String nonceHex, String dataHex) {
        if (!checkConnection()) {
            return null;
        }
        int[] path = Bip32Path.bip32StrToPath(pathStr);
        byte[] senderPublicKey = Convert.parseHexString(publicKeyHex);
        byte[] nonce = Convert.parseHexString(nonceHex);
        byte[] data = Convert.parseHexString(dataHex);
        DecryptedData decryptedData = decryptBuffer(path, senderPublicKey, new EncryptedData(data, nonce));
        return Convert.toHexString(decryptedData.getData()) + "," + Convert.toHexString(decryptedData.getSharedKey());
    }

    // not private for unit tests
    DecryptedData decryptBuffer(int[] path, byte[] targetPublicKey, EncryptedData encryptedData) {
        byte[] nonce = encryptedData.getNonce();
        if ((32 != targetPublicKey.length) || (0 != encryptedData.getData().length % 16) || (32 != nonce.length) || (2 > path.length)) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, encryptedData.getData()));
            return null;
        }
        byte[] bip32Path = Bip32Path.bip32PathToBytes(path);
        byte[] iv = new byte[16];
        System.arraycopy(encryptedData.getData(), 0, iv, 0, 16);
        byte[] encryptedBuffer = new byte[encryptedData.getData().length - iv.length];
        System.arraycopy(encryptedData.getData(), iv.length, encryptedBuffer, 0, encryptedBuffer.length);
        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        bytesStream.write(bip32Path, 0, bip32Path.length);
        bytesStream.write(targetPublicKey, 0, targetPublicKey.length);
        bytesStream.write(nonce, 0, nonce.length);
        bytesStream.write(iv, 0, iv.length);
        byte[] buffer = bytesStream.toByteArray();
        byte[] data = generateBuffer(INSTRUCTIONS.ENCRYPT_DECRYPT_MSG, P1_ENCRYPTION_DECRYPT_AND_RETURN_SHARED_KEY, buffer);
        byte[] result = exchange(data);
        if (isInvalidResult(result)) {
            return null;
        }

        byte[] sharedKey = getSharedKey(result);
        if (sharedKey == null) {
            return null;
        }

        byte[] message = getDecryptedMessage(encryptedBuffer);
        if (message == null) {
            return null;
        }
        byte[] unPaddedCompressedBytes = Convert.pkcs7Unpad(message);
        if (unPaddedCompressedBytes == null) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_PADDING, message));
            return null;
        }
        return new DecryptedData(Convert.uncompress(unPaddedCompressedBytes), sharedKey);
    }

    private byte[] getSharedKey(byte[] result) {
        byte[] sharedKey = new byte[32];
        if (1 + sharedKey.length != result.length) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
            return null;
        }
        System.arraycopy(result, 1, sharedKey, 0, sharedKey.length);
        return sharedKey;
    }

    @SuppressWarnings("ConstantConditions")
    private byte[] getDecryptedMessage(byte[] encryptedBuffer) {
        int pos = 0;
        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        while (pos < encryptedBuffer.length) {
            int endPos = Math.min(encryptedBuffer.length, pos + CHUNK_SIZE);
            byte[] chunk = new byte[endPos - pos];
            System.arraycopy(encryptedBuffer, pos, chunk, 0, chunk.length);
            byte[] result = exchange(generateBuffer(INSTRUCTIONS.ENCRYPT_DECRYPT_MSG, P1_ENCRYPTION_PROCESS_DATA, chunk));
            if (isInvalidResult(result)) {
                return null;
            }

            // if we aren't in position 0 then the ret value should be the same length as sent + 1
            if (endPos - pos + 1 != result.length) {
                setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
                return null;
            }
            bytesStream.write(result, 1, result.length - 1);
            pos = endPos;
        }
        return bytesStream.toByteArray();
    }

    public void showAddress(String pathStr) {
        if (checkConnection()) {
            showAddress(Bip32Path.bip32StrToPath(pathStr));
        }
    }

    private void showAddress(int[] path) {
        byte[] data = generateBuffer(INSTRUCTIONS.SHOW_PUBLIC_KEY, 0, Bip32Path.bip32PathToBytes(path));
        byte[] result = exchange(data);
        isInvalidResult(result);
        if (1 != result.length) {
            setLastError(new ArdorAppException(AppReturnCode.WRONG_MESSAGE_LENGTH, result));
        }
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    private void setLastError(Throwable t) {
        lastError = t.getLocalizedMessage();
        Logger.logInfoMessage(t.getMessage(), t);
    }

    private void clearLastError() {
        lastError = "";
    }
}
