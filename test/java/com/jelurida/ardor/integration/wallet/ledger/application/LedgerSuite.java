/*
 * Copyright © 2013-2016 The Nxt Core Developers.
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
import nxt.DoPrivilegedTestRule;
import nxt.http.AbstractHttpApiSuite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
        ArdorAppBridgeInteractiveTest.class,
        ArdorAppBridgeHeadlessTest.class,
        LedgerComparisonHeadlessTest.class
})
public class LedgerSuite extends AbstractHttpApiSuite {
    @ClassRule
    public static final DoPrivilegedTestRule DO_PRIVILEGED_TEST_RULE = new DoPrivilegedTestRule();

    @BeforeClass
    public static void init() {
        // To avoid permission exception when loading the JNA dll.
        BlockchainTest.putAdditionalProperty("nxt.disableSecurityPolicy", "true");
    }
}
