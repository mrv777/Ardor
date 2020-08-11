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

package nxt.util;

import nxt.Constants;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteOrder;

public class Bip32PathTest {

    public static final String PATH_STR = "m/44'/16754'/0'/1'/0";

    @Test
    public void validPathArrayToString() {
        Assert.assertEquals(PATH_STR, Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH.toString());
    }

    @Test
    public void validPathStringToArray() {
        Bip32Path bip32Path = Bip32Path.fromString(PATH_STR);
        Assert.assertEquals(Constants.ARDOR_TESTNET_BIP32_FIRST_CHILD_PATH, bip32Path);
    }

    @Test
    public void validatePath() {
        Assert.assertTrue(Bip32Path.validateString(PATH_STR));
        Assert.assertTrue(Bip32Path.validateString("M/44'/16754'/0/1/0"));
        Assert.assertFalse(Bip32Path.validateString("m/44'\\16754'/0/1/0"));
        Assert.assertFalse(Bip32Path.validateString("m/44'/16754q/0/1/0"));
        Assert.assertFalse(Bip32Path.validateString("m/44'/qqq/0/1/0"));
        Assert.assertFalse(Bip32Path.validateString("m/44'/0/1/0/qqq"));
    }

    @Test
    public void toByteArray() {
        Bip32Path bip32Path = Bip32Path.fromString(PATH_STR);
        Assert.assertEquals("2c00008072410080000000800100008000000000", Convert.toHexString(bip32Path.toByteArray(ByteOrder.LITTLE_ENDIAN)));
    }

}
