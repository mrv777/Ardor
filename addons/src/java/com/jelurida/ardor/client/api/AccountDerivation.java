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

package com.jelurida.ardor.client.api;

import nxt.Constants;
import nxt.account.Account;
import nxt.addons.JO;
import nxt.blockchain.Chain;
import nxt.crypto.Crypto;
import nxt.crypto.KeyDerivation;
import nxt.http.callers.DeriveAccountFromMasterPublicKeyCall;
import nxt.http.callers.DeriveAccountFromSeedCall;
import nxt.http.callers.SendMessageCall;
import nxt.util.Bip32Path;
import nxt.util.Convert;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Derive multiple account addresses from the same seed
 */
@SuppressWarnings("DuplicatedCode")
public class AccountDerivation {

    private static final String MNEMONIC = "opinion change copy struggle town cigar input kit school patient execute bird bundle option canvas defense hover poverty skill donkey pottery infant sense orchard";
    private final URL remoteUrl;
    private final Chain chain;

    private AccountDerivation(URL remoteUrl, Chain chain) {
        this.remoteUrl = remoteUrl;
        this.chain = chain;
    }

    public static void main(String[] args) throws MalformedURLException {
        URL remoteUrl = new URL("http://localhost:26876/nxt");
        Chain chain = Chain.getChain("IGNIS");
        AccountDerivation accountDerivation = new AccountDerivation(remoteUrl, chain);
        System.out.println("Calculate seed and derive accounts from mnemonic without passphrase");
        accountDerivation.deriveFromSeed(MNEMONIC, "", Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH.toPathArray());
        String passphrase = "Use random value difficult to guess and keep it secret and backed up";
        System.out.println("Calculate seed and derive accounts from mnemonic with passphrase: " + passphrase);
        accountDerivation.deriveFromSeed(MNEMONIC, passphrase, Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH.toPathArray());
        System.out.println("Derive parent key from seed then derive child private and public keys");
        accountDerivation.deriveFromParentNode(MNEMONIC, "", Constants.ARDOR_TESTNET_BIP32_ROOT_PATH.toPathArray());
        System.out.println("Derive parent key from seed and passphrase then derive child private and public keys: " + passphrase);
        accountDerivation.deriveFromParentNode(MNEMONIC, passphrase, Constants.ARDOR_TESTNET_BIP32_ROOT_PATH.toPathArray());
    }

    /**
     * Given mnemonic and bip32 path derive a sender account than increase the last index of the path by one and derive
     * a recipient account. Send message from sender to recipient.
     * @param mnemonic 12 to 24 secret random words compatible with bip39
     * @param passphrase optional random string combined with the mnemonic to support computing multiple seeds from the
     *                   same secret words. Not to be confused with account secret phrase.
     * @param bip32Path the bip32 path represented as int array
     */
    @SuppressWarnings("SameParameterValue")
    private void deriveFromSeed(String mnemonic, String passphrase, int[] bip32Path) {
        JO derivedSenderAccount = DeriveAccountFromSeedCall.create().
                mnemonic(mnemonic).
                passphrase(passphrase).
                bip32Path(Bip32Path.bip32PathToStr(bip32Path)).remote(remoteUrl).call();
        byte[] privateKey = derivedSenderAccount.parseHexString("privateKey");
        System.out.printf("Sender account %s\n", Convert.rsAccount(Account.getId(Crypto.getPublicKey(privateKey))));
        bip32Path = Arrays.copyOf(bip32Path, bip32Path.length); // Do not change the constant array
        bip32Path[bip32Path.length - 1]++;
        JO derivedRecipientAccount = DeriveAccountFromSeedCall.create().mnemonic(mnemonic).passphrase(passphrase).bip32Path(Bip32Path.bip32PathToStr(bip32Path)).remote(remoteUrl).call();
        byte[] publicKey = derivedRecipientAccount.parseHexString("publicKey");
        long recipientAccount = Account.getId(publicKey);
        System.out.printf("Recipient account %s\n", Convert.rsAccount(recipientAccount));

        JO response = SendMessageCall.create(chain.getId()).
                recipient(recipientAccount).
                recipientPublicKey(publicKey). // public key announcement to register the recipient public key in the blockchain
                message("hi").
                deadline(15).
                feeNQT(0).
                broadcast(true).
                privateKey(privateKey).
                remote(remoteUrl).
                call();
        System.out.println(response.toJSONString());
    }

    /**
     * Given mnemonic and bip32 path derive a parent node in the bip32 path then from the parent node derive the sender and
     * recipient account locally. Send message from sender to recipient.
     * Note that to derive the recipient address we use only the master public key and chain code, this can be done without
     * knowing the private key or seed.
     * @param mnemonic 12 to 24 secret random words compatible with bip39
     * @param passphrase optional random string combined with the mnemonic to support computing multiple seeds from the
     *                   same secret words. Not to be confused with account secret phrase.
     * @param bip32Path the bip32 path represented as int array
     */
    @SuppressWarnings("SameParameterValue")
    private void deriveFromParentNode(String mnemonic, String passphrase, int[] bip32Path) {
        JO parentNodeJson = DeriveAccountFromSeedCall.create().
                mnemonic(mnemonic).
                passphrase(passphrase).
                bip32Path(Bip32Path.bip32PathToStr(bip32Path)).remote(remoteUrl).call();

        byte[] privateKeyLeft = parentNodeJson.parseHexString("privateKey");
        byte[] privateKeyRight = parentNodeJson.parseHexString("privateKeyRight");
        byte[] masterPublicKey = parentNodeJson.parseHexString("masterPublicKey");
        byte[] chainCode = parentNodeJson.parseHexString("chainCode");
        KeyDerivation.Bip32Node parentNode = new KeyDerivation.Bip32Node(privateKeyLeft, privateKeyRight, masterPublicKey, chainCode, null);
        KeyDerivation.Bip32Node childNode = KeyDerivation.deriveChildPrivateKey(parentNode, 0);
        byte[] privateKey = childNode.getPrivateKeyLeft();
        System.out.printf("Sender account %s\n", Convert.rsAccount(Account.getId(Crypto.getPublicKey(privateKey))));

        JO recipientNode = DeriveAccountFromMasterPublicKeyCall.create().
                serializedMasterPublicKey(parentNode.getSerializedMasterPublicKey()).
                childIndex(1).
                remote(remoteUrl).call();

        byte[] publicKey = recipientNode.parseHexString("publicKey");
        long recipientAccount = Account.getId(publicKey);
        System.out.printf("Recipient account %s\n", Convert.rsAccount(recipientAccount));

        JO response = SendMessageCall.create(chain.getId()).
                recipient(recipientAccount).
                recipientPublicKey(publicKey). // public key announcement to register the recipient public key in the blockchain
                message("hi").
                deadline(15).
                feeNQT(0).
                broadcast(true).
                privateKey(privateKey).
                remote(remoteUrl).
                call();
        System.out.println(response.toJSONString());
    }
}
