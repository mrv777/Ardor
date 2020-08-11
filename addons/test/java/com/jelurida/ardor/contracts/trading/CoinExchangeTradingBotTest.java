package com.jelurida.ardor.contracts.trading;

import com.jelurida.ardor.contracts.AbstractContractTest;
import com.jelurida.ardor.contracts.ContractTestHelper;
import com.jelurida.ardor.contracts.OrderBean;
import nxt.addons.JO;
import nxt.blockchain.Chain;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.callers.GetAskOrdersCall;
import nxt.http.callers.GetBidOrdersCall;
import nxt.http.callers.GetCoinExchangeOrdersCall;
import nxt.http.callers.GetCoinExchangeTradesCall;
import nxt.http.callers.GetTradesCall;
import nxt.http.callers.TransferAssetCall;
import nxt.http.client.IssueAssetBuilder;
import nxt.http.client.IssueAssetBuilder.IssueAssetResult;
import nxt.http.responses.CoinExchangeOrderResponse;
import nxt.http.responses.CoinExchangeTradeResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.jelurida.ardor.contracts.trading.AssetOrderBean.assetBuy;
import static com.jelurida.ardor.contracts.trading.AssetOrderBean.assetSell;
import static com.jelurida.ardor.contracts.OrderBean.order;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.swap;
import static java.util.stream.Collectors.toList;
import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.FxtChain.FXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CoinExchangeTradingBotTest extends AbstractContractTest {

    private static final long ONE_IGNIS = IGNIS.ONE_COIN;

    @Before
    public void setUp() {
        assertEquals(1, getHeight());
    }

    @Test
    public void ardorIgnisBot() {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        List<OrderBean> bidOrders = asList(
                order(180, 100, FXT, IGNIS),
                order(160, 200, FXT, IGNIS));

        OrderBean.createOrderBook(bidOrders);

        List<OrderBean> askOrders = asList(
                order(45, 100, IGNIS, FXT),
                order(40, 200, IGNIS, FXT));
        OrderBean.createOrderBook(askOrders);
        generateBlock();

        assertEquals(bidOrders, getExchangeOrders(FXT, IGNIS));
        assertEquals(askOrders, getExchangeOrders(IGNIS, FXT));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        List<OrderBean> expectedUpdatedBidOrders = asList(
                order(181, 500, FXT, IGNIS),
                bidOrders.get(0),
                order(161, 1000, FXT, IGNIS),
                bidOrders.get(1));
        assertEquals(expectedUpdatedBidOrders, getExchangeOrders(FXT, IGNIS));

        List<OrderBean> expectedUpdatedAskOrders = asList(
                order(46, 200, IGNIS, FXT),
                askOrders.get(0),
                order(41, 400, IGNIS, FXT),
                askOrders.get(1));
        assertEquals(expectedUpdatedAskOrders, getExchangeOrders(IGNIS, FXT));

        assertNoTrades(FXT, IGNIS);

        generateBlock(); // Bot orders cancellation submitted
        generateBlock(); // Cancellations are confirmed, orders removed

        assertEquals(bidOrders, getExchangeOrders(FXT, IGNIS));
        assertEquals(askOrders, getExchangeOrders(IGNIS, FXT));

        generateBlock(); // new order are confirmed

        assertEquals(expectedUpdatedBidOrders, getExchangeOrders(FXT, IGNIS));
        assertEquals(expectedUpdatedAskOrders, getExchangeOrders(IGNIS, FXT));

        assertNoTrades(FXT, IGNIS);
    }

    @Test
    public void noExistingOrders() {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        generateBlock();


        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed


        assertEquals(
                asList(
                        order(190, 500, FXT, IGNIS),
                        order(170, 1000, FXT, IGNIS)),
                getExchangeOrders(FXT, IGNIS));

        assertEquals(
                asList(
                        order(0.475, 200, IGNIS, FXT),
                        order(0.425, 400, IGNIS, FXT)),
                getExchangeOrders(IGNIS, FXT));

        assertNoTrades(FXT, IGNIS);
    }

    @Test
    public void assetBotWithBothSides() {
        final long assetId = createAndDistributeAsset(8);
        generateBlock();

        setRunnerConfig(createAssetTBotConfig(assetId));
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        final long IGNIS_CENT = IGNIS.ONE_COIN / 100;
        final long ASSET_COIN = 100_000_000;
        List<AssetOrderBean> bidOrders = asList(
                assetBuy(180 * IGNIS_CENT, 100 * ASSET_COIN, assetId),
                assetBuy(160 * IGNIS_CENT, 200 * ASSET_COIN, assetId));

        createAssetOrderBook(bidOrders);

        List<AssetOrderBean> askOrders = asList(
                assetSell(250 * IGNIS_CENT, 200 * ASSET_COIN, assetId),
                assetSell(220 * IGNIS_CENT, 100 * ASSET_COIN, assetId));
        createAssetOrderBook(askOrders);
        generateBlock();

        assertEquals(bidOrders, getAssetExchangeBidOrders(assetId));
        assertEquals(askOrders, getAssetExchangeAskOrders(assetId));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        List<AssetOrderBean> expectedUpdatedBidOrders = asList(
                assetBuy(181 * IGNIS_CENT, 500 * ASSET_COIN, assetId),
                bidOrders.get(0),
                assetBuy(161 * IGNIS_CENT, 1000 * ASSET_COIN, assetId),
                bidOrders.get(1));
        assertEquals(expectedUpdatedBidOrders, getAssetExchangeBidOrders(assetId));

        List<AssetOrderBean> expectedUpdatedAskOrders = asList(
                askOrders.get(0),
                assetSell(243902439, 300 * ASSET_COIN, assetId), // 10E+8/(0.01 + 1/(2.5*10E+8*10E-8))
                askOrders.get(1),
                assetSell(215264190, 200 * ASSET_COIN, assetId)
                // price: 1/coin_price = 10E+8/(0.01 + 1/(2.2*10E+8*10E-8))
                // quantity: configured_quantity * coin_price = 200/price

        );
        assertEquals(expectedUpdatedAskOrders, getAssetExchangeAskOrders(assetId));

        assertAssetNoTrades(assetId);

        generateBlock(); // Bot orders cancellation submitted
        generateBlock(); // Cancellations are confirmed, orders removed

        assertEquals(bidOrders, getAssetExchangeBidOrders(assetId));
        assertEquals(askOrders, getAssetExchangeAskOrders(assetId));

        generateBlock(); // new order are confirmed

        assertEquals(expectedUpdatedBidOrders, getAssetExchangeBidOrders(assetId));
        assertEquals(expectedUpdatedAskOrders, getAssetExchangeAskOrders(assetId));

        assertAssetNoTrades(assetId);
    }

    @Test
    public void assetBotWith2DecimalsAssetBid() {
        final long assetId = createAndDistributeAsset(2);
        generateBlock();

        setRunnerConfig(createAssetTBotConfig(assetId));
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        final long IGNIS_CENT = IGNIS.ONE_COIN / 100;
        final long ASSET_COIN = 100;
        List<AssetOrderBean> bidOrders = asList(
                assetBuy(180 * IGNIS_CENT, 100 * ASSET_COIN, assetId),
                assetBuy(160 * IGNIS_CENT, 200 * ASSET_COIN, assetId));

        createAssetOrderBook(bidOrders);

        generateBlock();

        assertEquals(bidOrders, getAssetExchangeBidOrders(assetId));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        List<AssetOrderBean> expectedUpdatedBidOrders = asList(
                assetBuy(181 * IGNIS_CENT, 500 * ASSET_COIN, assetId),
                bidOrders.get(0),
                assetBuy(161 * IGNIS_CENT, 1000 * ASSET_COIN, assetId),
                bidOrders.get(1));
        assertEquals(expectedUpdatedBidOrders, getAssetExchangeBidOrders(assetId));

        assertAssetNoTrades(assetId);
    }

    @Test
    public void assetBotWith2DecimalsAssetAsk() {
        final long assetId = createAndDistributeAsset(2);
        generateBlock();

        setRunnerConfig(createAssetTBotConfig(assetId));
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        final long IGNIS_CENT = IGNIS.ONE_COIN / 100;
        final long ASSET_COIN = 100;

        List<AssetOrderBean> askOrders = asList(
                assetSell(250 * IGNIS_CENT, 200 * ASSET_COIN, assetId), //2.5 ignis
                assetSell(220 * IGNIS_CENT, 100 * ASSET_COIN, assetId)); // 2.2 ignis
        createAssetOrderBook(askOrders);
        generateBlock();

        assertEquals(askOrders, getAssetExchangeAskOrders(assetId));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        List<AssetOrderBean> expectedUpdatedAskOrders = asList(
                askOrders.get(0),
                assetSell(243902439, 300 * ASSET_COIN, assetId), // or 8_200
                // price: 10E+8 * 1/(0.01 + 1/(2.5*10E+8*10E-8))
                // quantity: 400 * 10E+2 * 1/(243902439*10E-8))
                askOrders.get(1),
//                assetSell(215264190, 9_291, assetId) // calculated
                assetSell(217391304, 200 * ASSET_COIN, assetId)
                // 2 * 10e+4
                // price: 1/coin_price = 10E+8 * 1/(0.01 + 1/(2.2*10E+8*10E-8))
                // quantity: configured_quantity * coin_price = 200/price(price in coins, not NQT!)

//                priceC of asset/coin * quantityC of coin
//                priceA of coin/asset = 1/priceC
//                quantityA of asset = priceC * quantityC = quantityC / priceA
        );
        assertEquals(expectedUpdatedAskOrders, getAssetExchangeAskOrders(assetId));

        assertAssetNoTrades(assetId);
    }

    @Test
    public void assetBotBidOnly() {
        final long janusXT = -3873996989158872743L;
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        List<AssetOrderBean> bidOrders = asList(
                assetBuy(180000, 10, janusXT),
                assetBuy(160000, 20, janusXT));

        createAssetOrderBook(bidOrders);

        generateBlock();

        assertEquals(bidOrders, getAssetExchangeBidOrders(janusXT));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        List<AssetOrderBean> expectedUpdatedBidOrders = asList(
                assetBuy(1180000, 50, janusXT),
                assetBuy(1160000, 100, janusXT),
                bidOrders.get(0),
                bidOrders.get(1));
        assertEquals(expectedUpdatedBidOrders, getAssetExchangeBidOrders(janusXT));

        assertAssetNoTrades(janusXT);

        generateBlock(); // Bot orders cancellation submitted
        generateBlock(); // Cancellations are confirmed, orders removed

        assertEquals(bidOrders, getAssetExchangeBidOrders(janusXT));

        generateBlock(); // new order are confirmed

        assertEquals(expectedUpdatedBidOrders, getAssetExchangeBidOrders(janusXT));

        assertAssetNoTrades(janusXT);
    }

    private void assertAssetNoTrades(long assetId) {
        final JSONObject json = GetTradesCall.create(IGNIS.getId()).asset(assetId).firstIndex(0).lastIndex(-1).build().invokeNoError();
        assertEquals(emptyList(), json.get("trades"));
    }

    private static List<AssetOrderBean> getAssetExchangeBidOrders(long asset) {
        JSONArray array = (JSONArray) GetBidOrdersCall.create(IGNIS.getId())
                .asset(asset)
                .build().invokeNoError()
                .get("bidOrders");
        return ((List<?>) array).stream().map(o -> AssetBuyBean.fromJSONObject((JSONObject) o)).collect(Collectors.toList());
    }

    private static List<AssetOrderBean> getAssetExchangeAskOrders(long asset) {
        JSONArray array = (JSONArray) GetAskOrdersCall.create(IGNIS.getId())
                .asset(asset)
                .build().invokeNoError()
                .get("askOrders");
        final List<AssetOrderBean> result = ((List<?>) array).stream().map(o -> AssetSellBean.fromJSONObject((JSONObject) o)).collect(toList());
        Collections.reverse(result);
        return result;
    }

    private void createAssetOrderBook(List<AssetOrderBean> orders) {
        orders.forEach(AssetOrderBean::placeOrder);
    }

    private List<OrderBean> getExchangeOrders(Chain fromChain, Chain toChain) {
        JSONObject response = GetCoinExchangeOrdersCall.create(fromChain.getId())
                .exchange(toChain.getId())
                .build()
                .invokeNoError();
        JO responseJo = new JO(response);
        System.out.println(responseJo);
        List<OrderBean> orders = responseJo.getJoList("orders").stream()
                .map(CoinExchangeOrderResponse::create)
                .map(OrderBean::new)
                .collect(toList());
        fixOrderInPlace(orders);
        return orders;
    }

    /*
     * For test stability only. For some reason orders which differ only in volume have unpredictable order.
     * Adding sorting by quantity.
     */

    private void fixOrderInPlace(List<OrderBean> orders) {
        if (orders.size() < 2) {
            return;
        }
        for (int i = 1; i < orders.size(); i++) {
            OrderBean prev = orders.get(i - 1);
            OrderBean cur = orders.get(i);
            if (cur.equalsIgnoreQuantity(prev) && cur.getQuantity() > prev.getQuantity()) {
                swap(orders, i - 1, i);
            }
        }
    }

    @Test
    public void ardorEuroBot() {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class);

        List<OrderBean> bidOrders = asList(
                order(180, 100, FXT, AEUR),
                order(160, 200, FXT, AEUR));
        OrderBean.createOrderBook(bidOrders);

        List<OrderBean> askOrders = asList(
                order(45, 100, AEUR, FXT),
                order(40, 200, AEUR, FXT)
        );
        OrderBean.createOrderBook(askOrders);
        generateBlock();

        assertEquals(bidOrders, getExchangeOrders(FXT, AEUR));

        assertEquals(askOrders, getExchangeOrders(AEUR, FXT));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        List<OrderBean> updatedBidOrders = asList(
                order(181, 500, FXT, AEUR),
                bidOrders.get(0),
                order(161, 1000, FXT, AEUR),
                bidOrders.get(1));

        assertEquals(updatedBidOrders, getExchangeOrders(FXT, AEUR));

        List<OrderBean> updatedAskOrders = asList(
                order(46, 200, AEUR, FXT),
                askOrders.get(0),
                order(41, 400, AEUR, FXT),
                askOrders.get(1));
        assertEquals(updatedAskOrders, getExchangeOrders(AEUR, FXT));

        assertNoTrades(FXT, AEUR);
    }

    @Test
    public void ignisEuroBot() {
        JO setupParams = new JO();
        // The first order book quantity of price 180 is very small (less than 10x of the bot price level quantity) so the bot will ignore it and
        // place its first order at 161 just above the second order of 160. So the first two bot orders are at 161
        List<OrderBean> expectedBids = asList(
                order(180, 1, IGNIS, AEUR),
                order(161, 1000, IGNIS, AEUR),
                order(161, 500, IGNIS, AEUR),
                order(160, 200, IGNIS, AEUR),
                order(141, 2000, IGNIS, AEUR),
                order(140, 300, IGNIS, AEUR),
                order(121, 4000, IGNIS, AEUR),
                order(120, 800, IGNIS, AEUR),
                order(101, 8000, IGNIS, AEUR),
                order(100, 1000, IGNIS, AEUR)
        );

        // Similarly, the second order book quantity of price 40 is very small (less than 10x of the bot price level quantity) so the bot will ignore it and
        // place its second order at 36 just about the 3rd order of 35. So the second and third bot orders are at 36
        List<OrderBean> expectedAsks = asList(
                order(46, 200, AEUR, IGNIS),
                order(45, 100, AEUR, IGNIS),
                order(40, 1, AEUR, IGNIS),
                order(36, 600, AEUR, IGNIS),
                order(36, 400, AEUR, IGNIS),
                order(35, 300, AEUR, IGNIS),
                order(31, 800, AEUR, IGNIS),
                order(30, 400, AEUR, IGNIS),
                order(26, 1000, AEUR, IGNIS),
                order(25, 500, AEUR, IGNIS)
        );

        ignisAeurBotImpl(setupParams, expectedBids, expectedAsks);
    }

    @Test
    public void ignisEuroBotNoPriceOptimization() {
        JO setupParams = new JO();
        setupParams.put("shouldOptimizePrice", "false");

        List<OrderBean> expectedBids = asList(
                order(190, 500, IGNIS, AEUR),
                order(180, 1, IGNIS, AEUR),
                order(170, 1000, IGNIS, AEUR),
                order(160, 200, IGNIS, AEUR),
                order(150, 2000, IGNIS, AEUR),
                order(140, 300, IGNIS, AEUR),
                order(130, 4000, IGNIS, AEUR),
                order(120, 800, IGNIS, AEUR),
                order(110, 8000, IGNIS, AEUR),
                order(100, 1000, IGNIS, AEUR)
        );

        List<OrderBean> expectedAsks = asList(
                new OrderBean(4750, 200 * ONE_IGNIS, AEUR, IGNIS),
                order(45, 100, AEUR, IGNIS),
                new OrderBean(4250, 400 * ONE_IGNIS, AEUR, IGNIS),
                order(40, 1, AEUR, IGNIS),
                new OrderBean(3750, 600 * ONE_IGNIS, AEUR, IGNIS),
                order(35, 300, AEUR, IGNIS),
                new OrderBean(3250, 800 * ONE_IGNIS, AEUR, IGNIS),
                order(30, 400, AEUR, IGNIS),
                new OrderBean(2750, 1000 * ONE_IGNIS, AEUR, IGNIS),
                order(25, 500, AEUR, IGNIS)
        );

        ignisAeurBotImpl(setupParams, expectedBids, expectedAsks);
    }

    @Test
    public void cancelOnly() {
        ExchangeCoinsCall.create(IGNIS.getId())
                .exchange(AEUR.getId())
                .quantityQNT(1)
                .priceNQTPerCoin(180 * IGNIS.ONE_COIN / 100)
                .secretPhrase(ALICE.getSecretPhrase()) // our contract runner
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();

        generateBlock();
        generateBlock();

        assertNotEquals(emptyList(), getExchangeOrders(IGNIS, AEUR));

        JO setupParams = new JO();
        setupParams.put("isCancelOnly", "true");
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class, setupParams);

        generateBlock();
        assertNoTrades(IGNIS, AEUR);

        assertEquals(emptyList(), getExchangeOrders(IGNIS, AEUR));
    }

    private void ignisAeurBotImpl(JO setupParams, List<OrderBean> expectedBids, List<OrderBean> expectedAsks) {
        ContractTestHelper.deployContract(CoinExchangeTradingBot.class, setupParams);

        List<OrderBean> bids = asList(
                order(180, 1, IGNIS, AEUR),
                order(160, 200, IGNIS, AEUR),
                order(140, 300, IGNIS, AEUR),
                order(120, 800, IGNIS, AEUR),
                order(100, 1000, IGNIS, AEUR)
        );
        OrderBean.createOrderBook(bids);

        List<OrderBean> asks = asList(
                order(45, 100, AEUR, IGNIS),
                order(40, 1, AEUR, IGNIS),
                order(35, 300, AEUR, IGNIS),
                order(30, 400, AEUR, IGNIS),
                order(25, 500, AEUR, IGNIS)
        );
        OrderBean.createOrderBook(asks);

        generateBlock();

        assertEquals(bids, getExchangeOrders(IGNIS, AEUR));
        assertEquals(asks, getExchangeOrders(AEUR, IGNIS));

        generateBlock(); // Trading bot processing new orders
        generateBlock(); // Submitted orders confirmed

        assertEquals(expectedBids, getExchangeOrders(IGNIS, AEUR));
        assertEquals(expectedAsks, getExchangeOrders(AEUR, IGNIS));

        assertNoTrades(IGNIS, AEUR);
    }

    private void assertNoTrades(Chain fromChain, Chain toChain) {
        int chain = fromChain.getId();
        int exchange = toChain.getId();
        JO response;
        response = GetCoinExchangeTradesCall.create(chain).exchange(exchange).call();
        List<CoinExchangeTradeResponse> trades = response.getJoList("trades").stream().map(CoinExchangeTradeResponse::create).collect(toList());
        assertEquals(0, trades.size());

        response = GetCoinExchangeTradesCall.create(exchange).exchange(chain).call();
        trades = response.getJoList("trades").stream().map(CoinExchangeTradeResponse::create).collect(toList());
        assertEquals(0, trades.size());
    }

    private JO createAssetTBotConfig(long assetId) {
        final JO result = AccessController.doPrivileged((PrivilegedAction<JO>) () -> {
            try (InputStream is = getClass().getResourceAsStream("./asset_config.json")) {
                JSONObject o = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
                return new JO(o);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
        result.getJo("params")
                .getJo("CoinExchangeTradingBot")
                .getArray("pairs")
                .get(0)
                .getJo("pairSymbols")
                .put("to", Long.toUnsignedString(assetId));
        return result;
    }

    static long createAndDistributeAsset(int decimals) {
        IssueAssetResult result = new IssueAssetBuilder(DAVE, "AssetT")
                .setQuantityQNT(1_000_000_000_000L)
                .setDecimals(decimals)
                .issueAsset();
        generateBlock();

        final long assetId = result.getAssetId();

        TransferAssetCall.create(IGNIS.getId())
                .asset(assetId)
                .secretPhrase(DAVE.getSecretPhrase())
                .recipient(5873880488492319831L)
                .quantityQNT(1_000 * 100_000_000L)
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();
        generateBlock();

        return assetId;
    }

    private void setRunnerConfig(JO botConfig) {
        final byte[] bytes = botConfig.toJSONString().getBytes();
        setRunnerConfig(bytes);
    }
}
