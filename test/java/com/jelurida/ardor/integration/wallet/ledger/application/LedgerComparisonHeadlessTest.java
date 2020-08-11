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

import nxt.Constants;
import nxt.crypto.Curve25519;
import nxt.crypto.KeyDerivation;
import nxt.util.Bip32Path;
import nxt.util.Convert;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class LedgerComparisonHeadlessTest extends AbstractArdorAppBridgeTest {

    @BeforeClass
    public static void initDevice() {
        app = ArdorAppBridge.getApp();
    }

    @Before
    public void checkDeviceStatus() {
        Assert.assertTrue(app.getLastError(), app instanceof ArdorAppBridge);
    }

    @Test
    public void compareRandomChilds() {
        Bip32Path rootPath = Constants.ARDOR_TESTNET_BIP32_ROOT_PATH;
        KeyDerivation.Bip32Node parentNode = KeyDerivation.deriveMnemonic(rootPath.toString(), MNEMONIC);
        Bip32Path derivedPath = rootPath.appendChild(0);
        Random r = new Random();
        int firstChild = Math.abs(r.nextInt());
        int numChild = 10;
        for (int i = firstChild; i < firstChild + numChild; i++) {
            KeyDerivation.Bip32Node softwareKeyData = KeyDerivation.deriveChildPrivateKey(parentNode, i);
            derivedPath = derivedPath.updateLastChild(i);
            Logger.logInfoMessage("Comparing path " + derivedPath.toString());
            PublicKeyData hardwarePublicKeyData = app.getPublicKeyData(derivedPath.toPathArray(), true);
            Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), softwareKeyData.getPublicKey());
            Assert.assertArrayEquals(hardwarePublicKeyData.getEd25519PublicKey(), softwareKeyData.getMasterPublicKey());
            Assert.assertArrayEquals(hardwarePublicKeyData.getChainCode(), softwareKeyData.getChainCode());

            KeyDerivation.Bip32Node childPublicKeyNode = KeyDerivation.deriveChildPublicKey(parentNode, i);
            byte[] curve25519PublicKey = childPublicKeyNode.getPublicKey();
            Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), curve25519PublicKey);
            Assert.assertArrayEquals(hardwarePublicKeyData.getEd25519PublicKey(), childPublicKeyNode.getMasterPublicKey());
            Assert.assertArrayEquals(hardwarePublicKeyData.getChainCode(), childPublicKeyNode.getChainCode());

            byte[] publicKey = new byte[32];
            Curve25519.keygen(publicKey, null, softwareKeyData.getPrivateKeyLeft());
            Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), publicKey);
        }
    }

    @Test
    public void compareRandomPath() {
        Bip32Path rootPath = Constants.ARDOR_TESTNET_BIP32_ROOT_PATH;
        int[] pathPrefix = Arrays.copyOf(rootPath.toPathArray(), 2);
        Random r = new Random();
        int counter = 0;
        while (counter < 10) {
            // Build some random nested path
            int[] path = new int[2 + 1 + r.nextInt(7)];
            System.arraycopy(pathPrefix, 0, path, 0, pathPrefix.length);
            for (int i=2; i<path.length; i++) {
                path[i] = r.nextInt();
            }
            counter++;
            String pathStr = Bip32Path.bip32PathToStr(path);
            Logger.logInfoMessage("Test %d comparing path: %s", counter, pathStr);

            // Test that we derive the same keys using hardware and software
            KeyDerivation.Bip32Node softwareKeyData = KeyDerivation.deriveMnemonic(pathStr, MNEMONIC);
            PublicKeyData hardwarePublicKeyData = app.getPublicKeyData(path, true);
            Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), softwareKeyData.getPublicKey());
            Assert.assertArrayEquals(hardwarePublicKeyData.getEd25519PublicKey(), softwareKeyData.getMasterPublicKey());
            Assert.assertArrayEquals(hardwarePublicKeyData.getChainCode(), softwareKeyData.getChainCode());

            // Test that the resulting key pair is a valid x25519 key pair
            byte[] publicKey = new byte[32];
            Curve25519.keygen(publicKey, null, softwareKeyData.getPrivateKeyLeft());
            Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), publicKey);
        }
    }

    @Test
    public void compareSingle() {
        Bip32Path rootPath = Constants.ARDOR_TESTNET_BIP32_ROOT_PATH;
        KeyDerivation.Bip32Node parentNode = KeyDerivation.deriveMnemonic(rootPath.toString(), MNEMONIC);
        Logger.logInfoMessage("parentNode: %s", parentNode);
        KeyDerivation.Bip32Node softwareKeyData = KeyDerivation.deriveChildPrivateKey(parentNode, 0);
        Logger.logInfoMessage("derivedNode: %s", softwareKeyData);
        Bip32Path derivedPath = rootPath.appendChild(0);
        Logger.logInfoMessage("Comparing path " + derivedPath.toString());
        PublicKeyData hardwarePublicKeyData = app.getPublicKeyData(derivedPath.toPathArray(), true);
        Logger.logInfoMessage("Software " + Convert.toHexString(softwareKeyData.getMasterPublicKey()));
        Logger.logInfoMessage("Hardware " + Convert.toHexString(hardwarePublicKeyData.getEd25519PublicKey()));
        Assert.assertArrayEquals(hardwarePublicKeyData.getEd25519PublicKey(), softwareKeyData.getMasterPublicKey());
        Assert.assertArrayEquals(hardwarePublicKeyData.getChainCode(), softwareKeyData.getChainCode());
        Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), softwareKeyData.getPublicKey());

        KeyDerivation.Bip32Node childPublicKeyNode = KeyDerivation.deriveChildPublicKey(parentNode, 0);
        Assert.assertArrayEquals(hardwarePublicKeyData.getChainCode(), childPublicKeyNode.getChainCode());
        Assert.assertArrayEquals(hardwarePublicKeyData.getEd25519PublicKey(), childPublicKeyNode.getMasterPublicKey());
        byte[] curve25519PublicKey = childPublicKeyNode.getPublicKey();
        Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), curve25519PublicKey);

        byte[] publicKey = new byte[32];
        Curve25519.keygen(publicKey, null, softwareKeyData.getPrivateKeyLeft());
        Assert.assertArrayEquals(hardwarePublicKeyData.getCurve25519PublicKey(), publicKey);
    }

}
