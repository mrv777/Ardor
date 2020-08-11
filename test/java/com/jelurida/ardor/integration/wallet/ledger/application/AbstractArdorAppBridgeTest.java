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

import nxt.BlockchainTest;
import org.junit.BeforeClass;

import java.util.Random;

public abstract class AbstractArdorAppBridgeTest extends BlockchainTest {

    protected static final int SIGNATURE_POSITION = 69;
    protected static final int SIGNATURE_LENGTH = 64;

    protected static final String PATH_STR_PARENT_1 = "m/44'/16754'/0'/1'";
    protected static final String PATH_STR_PARENT_2 = "m/44'/16754'/0'/2'";

    protected static final String PATH_STR_0 = "m/44'/16754'/0'/1'/0";
    protected static final String PATH_STR_3 = "m/44'/16754'/0'/1'/3";

    // Constants dependent on the ledger device seed
    // Enter here the 24 words used to initialize the ledger device you are using for testing
    protected static final String MNEMONIC = "opinion change copy struggle town cigar input kit school patient execute bird bundle option canvas defense hover poverty skill donkey pottery infant sense orchard";

    protected static final byte[] DATA_TO_ENCRYPT = new byte[64];
    static {
        Random r = new Random();
        r.nextBytes(DATA_TO_ENCRYPT);
    }

    protected static ArdorAppInterface app;

    @BeforeClass
    public static void init() {
        BlockchainTest.putAdditionalProperty("nxt.disableSecurityPolicy", "true");
        BlockchainTest.init();
    }
}
