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

package nxt.http.bundling;

import nxt.http.AbstractHttpApiSuite;
import nxt.peer.FeeRateCalculatorTest;
import nxt.peer.BundlerRateTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        RulesTest.class,
        AssetBundlerTest.class,
        CurrencyBundlerTest.class,
        PurchaseBundlerTest.class,
        TransactionTypeBundlerTest.class,
        BundlingFeesTest.class,
        FeeRateCalculatorTest.class,
        BundlerRateTest.class
})
public class BundlingSuite extends AbstractHttpApiSuite {
}
