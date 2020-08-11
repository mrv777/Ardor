package com.jelurida.ardor.contracts.trading;

import com.jelurida.ardor.contracts.AbstractContractTest;
import com.jelurida.ardor.contracts.ContractTestHelper;
import nxt.addons.JO;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.callers.GetCoinExchangeOrdersCall;
import nxt.http.callers.GetCoinExchangeTradesCall;
import nxt.http.responses.CoinExchangeOrderResponse;
import nxt.http.responses.CoinExchangeTradeResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class CoinExchangeTradingBotLegacyTest extends AbstractContractTest {

    public static final long ONE_ARDOR_CENT = FxtChain.FXT.ONE_COIN / 100;
    public static final long ONE_IGNIS_CENT = ChildChain.IGNIS.ONE_COIN / 100;
    public static final long ONE_EURO_CENT = ChildChain.AEUR.ONE_COIN / 100;
    public static final long ONE_ARDOR = FxtChain.FXT.ONE_COIN;
    public static final long ONE_IGNIS = ChildChain.IGNIS.ONE_COIN;
    public static final long ONE_EURO = ChildChain.AEUR.ONE_COIN;

    @Test
    public void ardorIgnisBot() {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        long[] bidQuantities = new long[]{100 * ONE_ARDOR, 200 * ONE_ARDOR};
        long[] bidPrices = new long[]{180 * ONE_ARDOR_CENT, 160 * ONE_ARDOR_CENT};
        createOrderBook(bidQuantities, FxtChain.FXT, bidPrices, ChildChain.IGNIS);
        long[] askQuantities = new long[]{100 * ONE_IGNIS, 200 * ONE_IGNIS};
        long[] askPrices = new long[]{45 * ONE_IGNIS_CENT, 40 * ONE_IGNIS_CENT};
        createOrderBook(askQuantities, ChildChain.IGNIS, askPrices, FxtChain.FXT);
        generateBlock();

        JO response = GetCoinExchangeOrdersCall.create(1).exchange(2).call();
        System.out.println(response);
        List<CoinExchangeOrderResponse> orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Assert.assertEquals(bidQuantities[i], orders.get(i).getQuantityQNT());
            Assert.assertEquals(bidPrices[i], orders.get(i).getBidNQTPerCoin());
        }

        response = GetCoinExchangeOrdersCall.create(2).exchange(1).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Assert.assertEquals(askQuantities[i], orders.get(i).getQuantityQNT());
            Assert.assertEquals(askPrices[i], orders.get(i).getBidNQTPerCoin());
        }

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        response = GetCoinExchangeOrdersCall.create(1).exchange(2).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length + 2, orders.size());
        long[] newBidPrices = new long[]{181 * ONE_ARDOR_CENT, 180 * ONE_ARDOR_CENT, 161 * ONE_ARDOR_CENT, 160 * ONE_ARDOR_CENT};
        for (int i = 0; i < orders.size(); i++) {
            CoinExchangeOrderResponse order = orders.get(i);
            Assert.assertEquals(newBidPrices[i], order.getBidNQTPerCoin());
        }

        response = GetCoinExchangeOrdersCall.create(2).exchange(1).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length + 2, orders.size());
        long[] newAskPrices = new long[]{46 * ONE_IGNIS_CENT, 45 * ONE_IGNIS_CENT, 41 * ONE_IGNIS_CENT, 40 * ONE_IGNIS_CENT};
        for (int i = 0; i < orders.size(); i++) {
            CoinExchangeOrderResponse order = orders.get(i);
            Assert.assertEquals(newAskPrices[i], order.getBidNQTPerCoin());
        }

        assertNoTrades(1, 2);

        generateBlock(); // Bot orders cancellation submitted
        generateBlock(); // Cancellations are confirmed, new order are created
        response = GetCoinExchangeOrdersCall.create(1).exchange(2).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length, orders.size());
        response = GetCoinExchangeOrdersCall.create(2).exchange(1).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length, orders.size());

        generateBlock(); // new order are confirmed
        response = GetCoinExchangeOrdersCall.create(1).exchange(2).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length + 2, orders.size());
        response = GetCoinExchangeOrdersCall.create(2).exchange(1).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length + 2, orders.size());

        assertNoTrades(1, 2);
    }

    @Test
    public void ardorEuroBot() {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        long[] bidQuantities = new long[]{100 * ONE_EURO, 200 * ONE_EURO};
        long[] bidPrices = new long[]{180 * ONE_ARDOR_CENT, 160 * ONE_ARDOR_CENT};
        createOrderBook(bidQuantities, FxtChain.FXT, bidPrices, ChildChain.AEUR);
        long[] askQuantities = new long[]{100 * ONE_ARDOR, 200 * ONE_ARDOR};
        long[] askPrices = new long[]{45 * ONE_EURO_CENT, 40 * ONE_EURO_CENT};
        createOrderBook(askQuantities, ChildChain.AEUR, askPrices, FxtChain.FXT);
        generateBlock();

        JO response = GetCoinExchangeOrdersCall.create(1).exchange(3).call();
        System.out.println(response);
        List<CoinExchangeOrderResponse> orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Assert.assertEquals(bidQuantities[i], orders.get(i).getQuantityQNT());
            Assert.assertEquals(bidPrices[i], orders.get(i).getBidNQTPerCoin());
        }

        response = GetCoinExchangeOrdersCall.create(3).exchange(1).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Assert.assertEquals(askQuantities[i], orders.get(i).getQuantityQNT());
            Assert.assertEquals(askPrices[i], orders.get(i).getBidNQTPerCoin());
        }

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        response = GetCoinExchangeOrdersCall.create(1).exchange(3).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length + 2, orders.size());
        long[] newBidPrices = new long[]{181 * ONE_ARDOR_CENT, 180 * ONE_ARDOR_CENT, 161 * ONE_ARDOR_CENT, 160 * ONE_ARDOR_CENT};
        for (int i = 0; i < orders.size(); i++) {
            CoinExchangeOrderResponse order = orders.get(i);
            Assert.assertEquals(newBidPrices[i], order.getBidNQTPerCoin());
        }

        response = GetCoinExchangeOrdersCall.create(3).exchange(1).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length + 2, orders.size());
        long[] newAskPrices = new long[]{46 * ONE_EURO_CENT, 45 * ONE_EURO_CENT, 41 * ONE_EURO_CENT, 40 * ONE_EURO_CENT};
        for (int i = 0; i < orders.size(); i++) {
            CoinExchangeOrderResponse order = orders.get(i);
            Assert.assertEquals(newAskPrices[i], order.getBidNQTPerCoin());
        }

        assertNoTrades(1, 3);
    }

    @Test
    public void ignisEuroBot() {
        JO setupParams = new JO();
        // The first order book quantity of price 180 is very small (less than 10x of the bot price level quantity) so the bot will ignore it and
        // place its first order at 161 just above the second order of 160. So the first two bot orders are at 161
        long[] expectedBotBidPrices = new long[]{180 * ONE_IGNIS_CENT, 161 * ONE_IGNIS_CENT, 161 * ONE_IGNIS_CENT, 160 * ONE_IGNIS_CENT, 141 * ONE_IGNIS_CENT, 140 * ONE_IGNIS_CENT, 121 * ONE_IGNIS_CENT, 120 * ONE_IGNIS_CENT, 101 * ONE_IGNIS_CENT, 100 * ONE_IGNIS_CENT};

        // Similarly, the second order book quantity of price 40 is very small (less than 10x of the bot price level quantity) so the bot will ignore it and
        // place its second order at 36 just about the 3rd order of 35. So the second and third bot orders are at 36
        long[] expectedBotAskPrices = new long[]{46 * ONE_EURO_CENT, 45 * ONE_EURO_CENT, 40 * ONE_EURO_CENT, 36 * ONE_EURO_CENT, 36 * ONE_EURO_CENT, 35 * ONE_EURO_CENT, 31 * ONE_EURO_CENT, 30 * ONE_EURO_CENT, 26 * ONE_EURO_CENT, 25 * ONE_EURO_CENT};
        ignisAeurBotImpl(setupParams, expectedBotBidPrices, expectedBotAskPrices);
    }

    @Test
    public void ignisEuroBotNoPriceOptimization() {
        JO setupParams = new JO();
        setupParams.put("shouldOptimizePrice", "false");
        long[] expectedBotBidPrices = new long[]{190 * ONE_IGNIS_CENT, 180 * ONE_IGNIS_CENT, 170 * ONE_IGNIS_CENT, 160 * ONE_IGNIS_CENT, 150 * ONE_IGNIS_CENT, 140 * ONE_IGNIS_CENT, 130 * ONE_IGNIS_CENT, 120 * ONE_IGNIS_CENT, 110 * ONE_IGNIS_CENT, 100 * ONE_IGNIS_CENT};
        long[] expectedBotAskPrices = new long[]{4750, 45 * ONE_EURO_CENT, 4250, 40 * ONE_EURO_CENT, 3750, 35 * ONE_EURO_CENT, 3250, 30 * ONE_EURO_CENT, 2750, 25 * ONE_EURO_CENT};
        ignisAeurBotImpl(setupParams, expectedBotBidPrices, expectedBotAskPrices);
    }

    private void ignisAeurBotImpl(JO setupParams, long[] expectedBotBidPrices, long[] expectedBotAskPrices) {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class, setupParams);

        long[] bidQuantities = new long[]{ONE_EURO, 200 * ONE_EURO, 300 * ONE_EURO, 800 * ONE_EURO, 1000 * ONE_EURO};
        long[] bidPrices = new long[]{180 * ONE_IGNIS_CENT, 160 * ONE_IGNIS_CENT, 140 * ONE_IGNIS_CENT, 120 * ONE_IGNIS_CENT, 100 * ONE_IGNIS_CENT};
        createOrderBook(bidQuantities, ChildChain.IGNIS, bidPrices, ChildChain.AEUR);
        long[] askQuantities = new long[]{100 * ONE_IGNIS, ONE_IGNIS, 300 * ONE_IGNIS, 400 * ONE_IGNIS, 500 * ONE_IGNIS};
        long[] askPrices = new long[]{45 * ONE_EURO_CENT, 40 * ONE_EURO_CENT, 35 * ONE_EURO_CENT, 30 * ONE_EURO_CENT, 25 * ONE_EURO_CENT};
        createOrderBook(askQuantities, ChildChain.AEUR, askPrices, ChildChain.IGNIS);
        generateBlock();

        JO response = GetCoinExchangeOrdersCall.create(2).exchange(3).call();
        System.out.println(response);
        List<CoinExchangeOrderResponse> orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(bidQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Assert.assertEquals(bidQuantities[i], orders.get(i).getQuantityQNT());
            Assert.assertEquals(bidPrices[i], orders.get(i).getBidNQTPerCoin());
        }

        response = GetCoinExchangeOrdersCall.create(3).exchange(2).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(askQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Assert.assertEquals(askQuantities[i], orders.get(i).getQuantityQNT());
            Assert.assertEquals(askPrices[i], orders.get(i).getBidNQTPerCoin());
        }

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        response = GetCoinExchangeOrdersCall.create(2).exchange(3).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(2 * bidQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            CoinExchangeOrderResponse order = orders.get(i);
            Assert.assertEquals(expectedBotBidPrices[i], order.getBidNQTPerCoin());
        }

        response = GetCoinExchangeOrdersCall.create(3).exchange(2).call();
        System.out.println(response);
        orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(Collectors.toList());
        Assert.assertEquals(2 * askQuantities.length, orders.size());
        for (int i = 0; i < orders.size(); i++) {
            CoinExchangeOrderResponse order = orders.get(i);
            Assert.assertEquals(expectedBotAskPrices[i], order.getBidNQTPerCoin());
        }

        assertNoTrades(2, 3);
    }

    private void assertNoTrades(int chain, int exchange) {
        JO response;
        response = GetCoinExchangeTradesCall.create(chain).exchange(exchange).call();
        List<CoinExchangeTradeResponse> trades = response.getJoList("trades").stream().map(CoinExchangeTradeResponse::create).collect(Collectors.toList());
        Assert.assertEquals(0, trades.size());

        response = GetCoinExchangeTradesCall.create(exchange).exchange(chain).call();
        trades = response.getJoList("trades").stream().map(CoinExchangeTradeResponse::create).collect(Collectors.toList());
        Assert.assertEquals(0, trades.size());
    }

    private static void createOrderBook(long[] quantities, Chain fromChain, long[] prices, Chain toChain) {
        if (quantities.length != prices.length) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < quantities.length; i++) {
            createExchangeOrder(quantities[i], fromChain, prices[i], toChain);
        }
    }

    private static void createExchangeOrder(long quantity, Chain fromChain, long price, Chain toChain) {
        long feeNQT;
        if (fromChain == FxtChain.FXT || toChain == FxtChain.FXT) {
            feeNQT = FxtChain.FXT.ONE_COIN / 2;
        } else {
            feeNQT = fromChain.ONE_COIN;
        }
        JO response = ExchangeCoinsCall.create(fromChain.getId()).exchange(toChain.getId()).
                quantityQNT(quantity).priceNQTPerCoin(price).secretPhrase(DAVE.getSecretPhrase()).feeNQT(feeNQT).call();
        System.out.println(response);
    }
}
