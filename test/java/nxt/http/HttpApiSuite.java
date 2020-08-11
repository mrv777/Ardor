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

package nxt.http;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        GetConstantsTest.class,
        SendMoneyTest.class,
        SendMessageTest.class,
        LeaseTest.class,
        MessageEncryptionTest.class,
        GetExpectedAskOrdersTest.class,
        APIInProcessConnectorTest.class,
        APITestServletTest.class,
        APIRemoteConnectorTest.class,
        BundleTransactionsTest.class,
        GetAskOrdersTest.class,
        MockedRequestTest.class
})
public class HttpApiSuite extends AbstractHttpApiSuite {
}
