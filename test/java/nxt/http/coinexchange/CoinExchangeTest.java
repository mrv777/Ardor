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

import nxt.BlockchainTest;
import nxt.Tester;
import nxt.http.APICall.InvocationError;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.callers.GetCoinExchangeOrderCall;
import nxt.http.callers.GetCoinExchangeTradesCall;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;

public class CoinExchangeTest extends BlockchainTest {

    @Test
    public void simpleExchange() {
        // Want to buy 25 AEUR with a maximum price of 4 IGNIS per AEUR
        // Convert the amount to IGNIS
        long displayAEURAmount = 25;
        long quantityQNT = displayAEURAmount * AEUR.ONE_COIN;
        long displayIgnisPerAEURPrice = 4;
        long priceNQT = displayIgnisPerAEURPrice * IGNIS.ONE_COIN;

        // Submit request to buy 25 AEUR with a maximum price of 4 IGNIS per AEUR
        // Quantity is denominated in AEUR and price is denominated in IGNIS per whole AEUR
        final JSONObject orderCreatedAlice = ExchangeCoinsCall.create(IGNIS.getId()).
                secretPhrase(ALICE.getSecretPhrase()).
                feeRateNQTPerFXT(IGNIS.ONE_COIN).
                exchange(AEUR.getId()).
                quantityQNT(quantityQNT).
                priceNQTPerCoin(priceNQT).
                build().invokeNoError();
        Logger.logDebugMessage("exchangeCoins: " + orderCreatedAlice);
        generateBlock();

        final JSONObject orderInfoAlice = GetCoinExchangeOrderCall.create().
                order(getOrderId(orderCreatedAlice)).
                build().invokeNoError();
        Assert.assertEquals(Long.toString(25 * AEUR.ONE_COIN), orderInfoAlice.get("quantityQNT"));
        Assert.assertEquals(Long.toString(4 * IGNIS.ONE_COIN), orderInfoAlice.get("bidNQTPerCoin"));
        Assert.assertEquals(Long.toString((long) (1.0 / 4 * AEUR.ONE_COIN)), orderInfoAlice.get("askNQTPerCoin"));

        // Want to buy 110 IGNIS with a maximum price of 1/4 AEUR per IGNIS
        // Quantity is denominated in IGNIS price is denominated in AEUR per whole IGNIS
        JSONObject orderCreatedBob = ExchangeCoinsCall.create(AEUR.getId()).
                secretPhrase(BOB.getSecretPhrase()).
                feeRateNQTPerFXT(AEUR.ONE_COIN).
                exchange(IGNIS.getId()).
                quantityQNT(100 * IGNIS.ONE_COIN + 10 * IGNIS.ONE_COIN).
                priceNQTPerCoin(AEUR.ONE_COIN / 4).
                build().invokeNoError();
        Logger.logDebugMessage("exchangeCoins: " + orderCreatedBob);
        generateBlock();

        final JSONObject orderInfoBob = GetCoinExchangeOrderCall.create().
                order(getOrderId(orderCreatedBob)).
                build().invokeNoError();
        Assert.assertEquals(Long.toString(10 * IGNIS.ONE_COIN), orderInfoBob.get("quantityQNT")); // leftover after the exchange of 100
        Assert.assertEquals(Long.toString((long) (0.25 * AEUR.ONE_COIN)), orderInfoBob.get("bidNQTPerCoin"));
        Assert.assertEquals(Long.toString(4 * IGNIS.ONE_COIN), orderInfoBob.get("askNQTPerCoin"));

        // Now look at the resulting trades
        final JSONObject tradesBobAeur = GetCoinExchangeTradesCall.create(AEUR.getId()).
                account(BOB.getRsAccount()).
                build().invokeNoError();
        Logger.logDebugMessage("GetCoinExchangeTrades: " + tradesBobAeur);

        // Bob received 100 IGNIS and paid 0.25 AEUR per IGNIS
        JSONArray trades = (JSONArray) tradesBobAeur.get("trades");
        JSONObject trade = (JSONObject) trades.get(0);
        Assert.assertEquals(AEUR.getId(), (int) (long) trade.get("chain"));
        Assert.assertEquals(IGNIS.getId(), (int) (long) trade.get("exchange"));
        Assert.assertEquals("" + (100 * IGNIS.ONE_COIN), trade.get("quantityQNT")); // IGNIS bought
        Assert.assertEquals("" + (long) (0.25 * AEUR.ONE_COIN), trade.get("priceNQTPerCoin")); // AEUR per IGNIS price

        JSONObject tradesAliceIgnis = GetCoinExchangeTradesCall.create(IGNIS.getId()).
                account(ALICE.getRsAccount()).
                build().invokeNoError();
        Logger.logDebugMessage("GetCoinExchangeTrades: " + tradesAliceIgnis);

        // Alice received 25 AEUR and paid 4 IGNIS per AEUR
        trades = (JSONArray) tradesAliceIgnis.get("trades");
        trade = (JSONObject) trades.get(0);
        Assert.assertEquals(IGNIS.getId(), (int) (long) trade.get("chain"));
        Assert.assertEquals(AEUR.getId(), (int) (long) trade.get("exchange"));
        Assert.assertEquals("" + (25 * AEUR.ONE_COIN), trade.get("quantityQNT")); // AEUR bought
        Assert.assertEquals("" + (4 * IGNIS.ONE_COIN), trade.get("priceNQTPerCoin")); // IGNIS per AEUR price

        Assert.assertEquals(-100 * IGNIS.ONE_COIN - IGNIS.ONE_COIN / 100, ALICE.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(25 * AEUR.ONE_COIN, ALICE.getChainBalanceDiff(AEUR.getId()));
        Assert.assertEquals(100 * IGNIS.ONE_COIN, BOB.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(-25 * AEUR.ONE_COIN - AEUR.ONE_COIN / 100, BOB.getChainBalanceDiff(AEUR.getId()));
    }

    @Test
    public void multiOrderExchange() {
        // Want to buy 25 AEUR with a maximum price of 4 IGNIS per AEUR
        // Convert the amount to IGNIS
        long displayAEURAmount = 25;
        long quantityQNT = displayAEURAmount * AEUR.ONE_COIN;
        long displayIgnisPerAEURPrice = 4;
        long priceNQT = displayIgnisPerAEURPrice * IGNIS.ONE_COIN;

        // Submit request to buy 25 AEUR with a maximum price of 4 IGNIS per AEUR
        // Quantity is denominated in AEUR and price is denominated in IGNIS per whole AEUR
        final JSONObject orderCreatedAlice = ExchangeCoinsCall.create(IGNIS.getId()).
                secretPhrase(ALICE.getSecretPhrase()).
                feeRateNQTPerFXT(IGNIS.ONE_COIN).
                exchange(AEUR.getId()).
                quantityQNT(quantityQNT).
                priceNQTPerCoin(priceNQT).
                build().invokeNoError();
        Logger.logDebugMessage("exchangeCoins: " + orderCreatedAlice);
        generateBlock();


        // Want to buy 110 IGNIS with a maximum price of 1/4 AEUR per IGNIS
        // Quantity is denominated in IGNIS price is denominated in AEUR per whole IGNIS
        // Two orders to have two operations
        JSONObject orderCreatedBob = ExchangeCoinsCall.create(AEUR.getId()).
                secretPhrase(BOB.getSecretPhrase()).
                feeRateNQTPerFXT(AEUR.ONE_COIN).
                exchange(IGNIS.getId()).
                quantityQNT(50 * IGNIS.ONE_COIN + 10 * IGNIS.ONE_COIN).
                priceNQTPerCoin(AEUR.ONE_COIN / 4).
                build().invokeNoError();
        Logger.logDebugMessage("exchangeCoins: " + orderCreatedBob);
        generateBlock();

        JSONObject orderCreatedBob2 = ExchangeCoinsCall.create(AEUR.getId()).
                secretPhrase(BOB.getSecretPhrase()).
                feeRateNQTPerFXT(AEUR.ONE_COIN).
                exchange(IGNIS.getId()).
                quantityQNT(50 * IGNIS.ONE_COIN + 10 * IGNIS.ONE_COIN).
                priceNQTPerCoin(AEUR.ONE_COIN / 4).
                build().invokeNoError();
        Logger.logDebugMessage("exchangeCoins: " + orderCreatedBob2);
        generateBlock();


        // Now look at the resulting trades
        final JSONObject tradesBobAeur = GetCoinExchangeTradesCall.create(AEUR.getId()).
                account(BOB.getRsAccount()).
                build().invokeNoError();
        Logger.logDebugMessage("GetCoinExchangeTrades Bob AEUR: " + tradesBobAeur);

        // Bob received 100 IGNIS and paid 0.25 AEUR per IGNIS
        final JSONArray tradesArrayBob = (JSONArray) tradesBobAeur.get("trades");
        final JSONObject tradeBob1 = (JSONObject) tradesArrayBob.get(0);
        Assert.assertEquals(AEUR.getId(), (int) (long) tradeBob1.get("chain"));
        Assert.assertEquals(IGNIS.getId(), (int) (long) tradeBob1.get("exchange"));
        Assert.assertEquals("" + (40 * IGNIS.ONE_COIN), tradeBob1.get("quantityQNT")); // IGNIS bought
        Assert.assertEquals("" + (long) (0.25 * AEUR.ONE_COIN), tradeBob1.get("priceNQTPerCoin")); // AEUR per IGNIS price

        final JSONObject tradeBob2 = (JSONObject) tradesArrayBob.get(1);
        Assert.assertEquals(AEUR.getId(), (int) (long) tradeBob2.get("chain"));
        Assert.assertEquals(IGNIS.getId(), (int) (long) tradeBob2.get("exchange"));
        Assert.assertEquals("" + (60 * IGNIS.ONE_COIN), tradeBob2.get("quantityQNT")); // IGNIS bought
        Assert.assertEquals("" + (long) (0.25 * AEUR.ONE_COIN), tradeBob2.get("priceNQTPerCoin")); // AEUR per IGNIS price

        JSONObject tradesAliceIgnis = GetCoinExchangeTradesCall.create(IGNIS.getId()).
                account(ALICE.getRsAccount()).
                build().invokeNoError();
        Logger.logDebugMessage("GetCoinExchangeTrades Alice IGNIS: " + tradesAliceIgnis);

        // Alice received 25 AEUR and paid 4 IGNIS per AEUR
        JSONArray tradesArrayAlice = (JSONArray) tradesAliceIgnis.get("trades");
        final JSONObject tradeAlice1 = (JSONObject) tradesArrayAlice.get(0);
        Assert.assertEquals(IGNIS.getId(), (int) (long) tradeAlice1.get("chain"));
        Assert.assertEquals(AEUR.getId(), (int) (long) tradeAlice1.get("exchange"));
        Assert.assertEquals("" + (10 * AEUR.ONE_COIN), tradeAlice1.get("quantityQNT")); // AEUR bought
        Assert.assertEquals("" + (4 * IGNIS.ONE_COIN), tradeAlice1.get("priceNQTPerCoin")); // IGNIS per AEUR price

        final JSONObject tradeAlice2 = (JSONObject) tradesArrayAlice.get(1);
        Assert.assertEquals(IGNIS.getId(), (int) (long) tradeAlice2.get("chain"));
        Assert.assertEquals(AEUR.getId(), (int) (long) tradeAlice2.get("exchange"));
        Assert.assertEquals("" + (15 * AEUR.ONE_COIN), tradeAlice2.get("quantityQNT")); // AEUR bought
        Assert.assertEquals("" + (4 * IGNIS.ONE_COIN), tradeAlice2.get("priceNQTPerCoin")); // IGNIS per AEUR price

        Assert.assertEquals(-100 * IGNIS.ONE_COIN - IGNIS.ONE_COIN / 100, ALICE.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(25 * AEUR.ONE_COIN, ALICE.getChainBalanceDiff(AEUR.getId()));
        Assert.assertEquals(100 * IGNIS.ONE_COIN, BOB.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(-25 * AEUR.ONE_COIN - 2 * AEUR.ONE_COIN / 100, BOB.getChainBalanceDiff(AEUR.getId()));
    }

    private String getOrderId(JSONObject orderCreatedBob) {
        return Tester.responseToStringId((JSONObject) orderCreatedBob.get("transactionJSON"));
    }

    @Test
    public void ronsSample() {
        long AEURToBuy = 5 * AEUR.ONE_COIN;
        long ignisPerWholeAEUR = (long) (0.75 * IGNIS.ONE_COIN);

        JSONObject response = ExchangeCoinsCall.create(IGNIS.getId()).
                secretPhrase(ALICE.getSecretPhrase()).
                feeRateNQTPerFXT(IGNIS.ONE_COIN).
                exchange(AEUR.getId()).
                quantityQNT(AEURToBuy).
                priceNQTPerCoin(ignisPerWholeAEUR).
                build().invokeNoError();
        String aliceOrder = Tester.responseToStringId(response);
        generateBlock();

        long ignisToBuy = 5 * IGNIS.ONE_COIN;
        long AEURPerWholeIgnis = (long) (1.35 * AEUR.ONE_COIN);

        response = ExchangeCoinsCall.create(AEUR.getId()).
                secretPhrase(BOB.getSecretPhrase()).
                feeRateNQTPerFXT(AEUR.ONE_COIN).
                exchange(IGNIS.getId()).
                quantityQNT(ignisToBuy).
                priceNQTPerCoin(AEURPerWholeIgnis).
                build().invokeNoError();
        String bobOrder = Tester.responseToStringId(response);
        generateBlock();

        Assert.assertEquals((long) (-3.75 * IGNIS.ONE_COIN) - IGNIS.ONE_COIN / 100, ALICE.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(5 * AEUR.ONE_COIN, ALICE.getChainBalanceDiff(AEUR.getId()));
        Assert.assertEquals((long) (3.75 * IGNIS.ONE_COIN), BOB.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(-5 * AEUR.ONE_COIN - AEUR.ONE_COIN / 100, BOB.getChainBalanceDiff(AEUR.getId()));

        InvocationError aliceOrderInfo = GetCoinExchangeOrderCall.create().
                order(aliceOrder).
                build().invokeWithError();
        Assert.assertEquals("Unknown order", aliceOrderInfo.getErrorDescription());

        response = GetCoinExchangeOrderCall.create().
                order(bobOrder).
                build().invokeNoError();
        Assert.assertEquals((long) (1.25 * IGNIS.ONE_COIN), Long.parseLong((String) response.get("quantityQNT")));
        Assert.assertEquals((long) (1.35 * AEUR.ONE_COIN), Long.parseLong((String) response.get("bidNQTPerCoin")));
        Assert.assertEquals((long) (0.74074074 * IGNIS.ONE_COIN), Long.parseLong((String) response.get("askNQTPerCoin")));
    }
}
