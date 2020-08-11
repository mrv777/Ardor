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

QUnit.module("ledger.wallet");

/*
 * These tests require a hardware wallet.
 *
 * By default they are disabled (via QUnit.skip). To enable the tests change
 * the assignment to the "test" local variable below.
 *
 * To access USB devices via WebUSB you need to be on a secure context.
 * Therefore, configure your node to use Https.
 *
 * Opening WebUSB connection requires a "user gesture". Code like unit tests, which
 * is not triggered by a user gesture (like a mouse click) won't find any device.
 *
 * To workaround this, follow these steps:
 * 1. Make sure the ledger device is connected, unlocked and that the Ardor app is
 *    open.
 * 2. Start the wallet itself in a browser tab, connect to the device by loading
 *    accounts from the hardware tab and select your device in the native
 *    device selection dialog.
 * 3. Without closing anything, paste the unit tests URL, typically:
 *    https://localhost:26877/qunit.html?module=ledger.wallet to the same browser tab.
 * 4. You should see the tests executing in the browser.
 *
 * If you are getting DEVICE_NOT_SELECTED or DEVICE_USED_BY_ANOTHER_SERVICE errors
 * you are probably doing something wrong.
 */

(function(){
    const test = QUnit.test; // change to QUnit.test to enable the tests

    test("ledgerEncryptDecrypt", async assert => {
        const oldBip32Account = NRS.bip32Account;

        try {
            const bip32Account0 = await createBip32AccountFromHardwareWallet(0);
            const bip32Account3 = await createBip32AccountFromHardwareWallet(3);
            NRS.bip32Account = bip32Account0;
            const plainText = "LYLY";
            let response = await NRS.encryptUsingHardwareWallet(bip32Account3.getPublicKey(), plainText);
            assert.ok(response && response.message, "encryption response with message");
            assert.ok(response.nonce, "encryption response with nonce");

            NRS.bip32Account = bip32Account3;
            response = await NRS.decryptUsingHardwareWallet(response.message, {
                publicKey: bip32Account0.getPublicKey(),
                nonce: response.nonce
            });
            assert.equal(response.message, plainText, "decrypted text matches original");
        } finally {
            NRS.bip32Account = oldBip32Account;
        }
    });

    test("ledgerEncryptDecryptLongMessage", async assert => {
        const oldBip32Account = NRS.bip32Account;

        try {
            const bip32Account0 = await createBip32AccountFromHardwareWallet(0);
            const bip32Account3 = await createBip32AccountFromHardwareWallet(3);
            NRS.bip32Account = bip32Account0;
            const dataToEncrypt = new Uint8Array(1024);
            window.crypto.getRandomValues(dataToEncrypt);
            let response = await NRS.encryptUsingHardwareWallet(bip32Account3.getPublicKey(), dataToEncrypt);
            assert.ok(response && response.message, "encryption response with message");
            assert.ok(response.nonce, "encryption response with nonce");

            NRS.bip32Account = bip32Account3;
            response = await NRS.decryptUsingHardwareWallet(response.message, {
                publicKey: bip32Account0.getPublicKey(),
                nonce: response.nonce,
                isText: false
            });
            assert.deepEqual(response.message, converters.byteArrayToHexString(dataToEncrypt), "decrypted data matches original");
        } finally {
            NRS.bip32Account = oldBip32Account;
        }
    });

    test("generateToken", async assert => {
        const oldBip32Account = NRS.bip32Account;

        try {
            NRS.bip32Account = await createBip32AccountFromHardwareWallet(0);
            const timestamp = NRS.toEpochTime();
            const tokenData = "Token Data";
            let response = await NRS.signTokenOnHardwareWallet(timestamp, converters.stringToHexString(tokenData));
            assert.ok(response !== null, "token generated");
            // no code available to verify a token client-side
        } finally {
            NRS.bip32Account = oldBip32Account;
        }
    });

    test("generateTokenFromLargeDataSet", async assert => {
        const oldBip32Account = NRS.bip32Account;

        try {
            NRS.bip32Account = await createBip32AccountFromHardwareWallet(3);
            const timestamp = NRS.toEpochTime();
            const tokenData = new Uint8Array(40000);
            window.crypto.getRandomValues(tokenData);
            let response = await NRS.signTokenOnHardwareWallet(timestamp, converters.byteArrayToHexString(tokenData));
            assert.ok(response !== null, "token generated");
            // no code available to verify a token client-side
        } finally {
            NRS.bip32Account = oldBip32Account;
        }
    });

    async function createBip32AccountFromHardwareWallet(index) {
        const path = `m/44'/16754'/0'/1'/${index}`;
        const response = await NRS.getPublicKeyFromHardwareWallet(path, false);
        const publicKey = converters.byteArrayToHexString(response);
        console.log(`Public key for index ${index}: ${publicKey}`);
        return NRS.createBip32Account(NRS.BIP32_PROVIDER.LEDGER_HARDWARE, null, publicKey,
            NRS.getAccountIdFromPublicKey(publicKey, true), path, true);
    }
}());