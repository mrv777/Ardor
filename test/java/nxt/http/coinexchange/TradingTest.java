/*
 * Copyright © 2020 Jelurida IP B.V.
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

import nxt.BlockchainTest;
import nxt.addons.JO;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.callers.GetCoinExchangeOrdersCall;
import nxt.http.responses.CoinExchangeOrderResponse;
import nxt.util.Convert;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;

public class TradingTest extends BlockchainTest {

    @Test
    public void coinExchangeTrading() {
        JO response = ExchangeCoinsCall.create(2).exchange(3).quantityQNT(100).priceNQTPerCoin(5000000).
                feeRateNQTPerFXT(IGNIS.ONE_COIN).secretPhrase(ALICE.getSecretPhrase()).call();
        System.out.println(response);
        response = ExchangeCoinsCall.create(2).exchange(3).quantityQNT(200).priceNQTPerCoin(4000000).
                feeNQT(100000000).secretPhrase(ALICE.getSecretPhrase()).call();
        System.out.println(response);
        generateBlock();
        List<JO> ordersJson = GetCoinExchangeOrdersCall.create(2).exchange(3).call().getJoList("orders");
        List<CoinExchangeOrderResponse> orders = ordersJson.stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(orders.size(), 2);
        CoinExchangeOrderResponse order1 = orders.get(0);
        CoinExchangeOrderResponse order2 = orders.get(1);
        Assert.assertEquals(Convert.toBigDecimal(5000000, (byte) 8), order1.getBidPerCoin((byte)IGNIS.getDecimals()));
        Assert.assertEquals(Convert.toBigDecimal(200000, (byte) 4), order1.getAskPerCoin((byte)AEUR.getDecimals()));
        Assert.assertEquals(Convert.toBigDecimal(4000000, (byte) 8), order2.getBidPerCoin((byte)IGNIS.getDecimals()));
        Assert.assertEquals(Convert.toBigDecimal(250000, (byte) 4), order2.getAskPerCoin((byte)AEUR.getDecimals()));
    }

}
