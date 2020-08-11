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

package nxt.http;

import nxt.BlockchainTest;
import nxt.blockchain.FxtChain;
import nxt.http.APICall.Builder;
import nxt.http.callers.SendMoneyCall;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static nxt.http.CustomSensitiveParameterAddOn.handlerName;
import static nxt.http.CustomSensitiveParameterAddOn.parameterName;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class APIInProcessConnectorTest extends BlockchainTest {
    @Rule
    public final LogListeningRule logListeningRule = new LogListeningRule();

    @Test
    public void testSensitiveParametersHiding() {
        SendMoneyCall.create(FxtChain.FXT.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .recipient(BOB.getStrId())
                .amountNQT(100 * FxtChain.FXT.ONE_COIN)
                .feeNQT(FxtChain.FXT.ONE_COIN * 10)
                .build().invokeNoError();
        String actual = String.join("\n", logListeningRule.getMessages());

        assertThat(actual, containsString("secretPhrase={hidden}"));
        assertThat(actual, not(containsString("secretPhrase=hope"))); // part of password
    }

    @Test
    public void testCustomSensitiveParametersHiding() {
        new Builder<>(handlerName, singletonList(parameterName), null, false)
                .param(parameterName, "some value")
                .build().invokeNoError();
        String actual = String.join("\n", logListeningRule.getMessages());

        assertThat(actual, containsString(parameterName + "={hidden}"));
        assertThat(actual, not(containsString(parameterName + "=some value")));
    }
}