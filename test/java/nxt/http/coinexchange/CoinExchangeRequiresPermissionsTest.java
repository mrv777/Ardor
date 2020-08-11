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

package nxt.http.coinexchange;

import nxt.BlockchainWithChildChainControlTest;
import nxt.Tester;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.http.APICall.InvocationError;
import nxt.http.callers.ExchangeCoinsCall;
import org.junit.Before;
import org.junit.Test;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.removePermission;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static org.junit.Assert.assertEquals;

public class CoinExchangeRequiresPermissionsTest extends BlockchainWithChildChainControlTest {
    private Tester tester;
    private ChildChain childChain;
    private ChildChain exchangeChildChain;
    private String expectedErrorDescription;

    @Before
    public void setUp() {
        generateBlock();

        tester = ALICE;
        childChain = IGNIS;
        exchangeChildChain = AEUR;
        expectedErrorDescription = "User " + tester.getRsAccount() + " needs permission CHAIN_USER";
    }

    @Test
    public void testExchangeCoinsRequirePermissionsOnChain() {
        removePermission(childChain, tester, CHAIN_USER);

        InvocationError response = submitRequestWithError(childChain, exchangeChildChain);

        assertEquals(expectedErrorDescription, response.getErrorDescription());
    }

    @Test
    public void testExchangeToFxtCoins() {
        removePermission(childChain, tester, CHAIN_USER);

        InvocationError response = submitRequestWithError(childChain, FxtChain.FXT);

        assertEquals(expectedErrorDescription, response.getErrorDescription());
    }

    @Test
    public void testExchangeCoinsRequirePermissionsOnExchangeChain() {
        removePermission(exchangeChildChain, tester, CHAIN_USER);

        InvocationError response = submitRequestWithError(childChain, exchangeChildChain);

        assertEquals(expectedErrorDescription, response.getErrorDescription());
    }

    @Test
    public void testExchangeFxtCoinsRequirePermissionsOnExchangeChain() {
        removePermission(exchangeChildChain, tester, CHAIN_USER);

        InvocationError response = submitRequestWithError(FxtChain.FXT, exchangeChildChain);

        assertEquals(expectedErrorDescription, response.getErrorDescription());
    }

    private InvocationError submitRequestWithError(Chain from, Chain to) {
        return ExchangeCoinsCall.create(from.getId())
                .secretPhrase(tester.getSecretPhrase())
                .feeRateNQTPerFXT(from.ONE_COIN)
                .exchange(to.getId())
                .quantityQNT((long) 25 * AEUR.ONE_COIN)
                .priceNQTPerCoin((long) 4 * childChain.ONE_COIN)
                .build()
                .invokeWithError();
    }
}
