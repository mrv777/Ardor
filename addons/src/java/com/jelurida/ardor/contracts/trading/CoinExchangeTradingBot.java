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

package com.jelurida.ardor.contracts.trading;

import nxt.addons.AbstractContract;
import nxt.addons.AbstractContractContext;
import nxt.addons.BlockContext;
import nxt.addons.ChainWrapper;
import nxt.addons.ContractParametersProvider;
import nxt.addons.ContractRunnerParameter;
import nxt.addons.ContractSetupParameter;
import nxt.addons.InitializationContext;
import nxt.addons.JO;
import nxt.addons.ShutdownContext;
import nxt.addons.ValidateBlockchainIsUpToDate;
import nxt.http.APICall;
import nxt.http.callers.CancelAskOrderCall;
import nxt.http.callers.CancelBidOrderCall;
import nxt.http.callers.CancelCoinExchangeCall;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.callers.GetAccountCall;
import nxt.http.callers.GetAccountCurrentAskOrdersCall;
import nxt.http.callers.GetAccountCurrentBidOrdersCall;
import nxt.http.callers.GetAskOrdersCall;
import nxt.http.callers.GetAssetCall;
import nxt.http.callers.GetBalanceCall;
import nxt.http.callers.GetBidOrdersCall;
import nxt.http.callers.GetCoinExchangeOrdersCall;
import nxt.http.callers.PlaceAskOrderCall;
import nxt.http.callers.PlaceBidOrderCall;
import nxt.http.responses.CoinExchangeOrderResponse;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class CoinExchangeTradingBot extends AbstractContract<Object, Object> {

    // Effectively final - set these vars in init() and don't change them later
    private ExchangeApi exchangeApi;
    private CMCApi cmcApi;
    private boolean isCancelOnly;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @ContractParametersProvider
    public interface Params {

        @ContractRunnerParameter
        String cmcUrl();

        @ContractRunnerParameter
        String cmcApiKey();

        @ContractRunnerParameter
        String cmcFromSymbols();

        @ContractRunnerParameter
        String cmcToSymbols();

        @ContractRunnerParameter
        default long cmcRefreshInterval() {
            return 10L;
        }

        @ContractSetupParameter
        @ContractRunnerParameter
        default boolean shouldOptimizePrice() {
            return true;
        }

        @ContractSetupParameter
        @ContractRunnerParameter
        default boolean isCancelOnly() {
            return false;
        }
    }

    @Override
    public void init(InitializationContext context) {
        Params params = context.getParams(Params.class);
        isCancelOnly = params.isCancelOnly();
        Logger.logInfoMessage("isCancelOnly : %s", isCancelOnly);
        exchangeApi = new SelectExchangeApi(new CoinExchangeApi(), new AssetExchangeApi());
        cmcApi = new CMCApi(scheduler, params.cmcUrl(), params.cmcApiKey(), params.cmcFromSymbols(), params.cmcToSymbols(), params.cmcRefreshInterval());
    }

    @Override
    public void shutdown(ShutdownContext context) {
        Logger.logInfoMessage("%s shutting down", getClass().getSimpleName());
        context.shutdown(scheduler);
    }

    @Override
    @ValidateBlockchainIsUpToDate
    public JO processBlock(BlockContext context) {
        JO contractRunnerConfigParams = context.getContractRunnerConfigParams(this.getClass().getSimpleName());
        for (PairParams pairParams : getPairs(contractRunnerConfigParams)) {
            if (context.getHeight() % pairParams.refreshInterval > 1) {
                continue;
            }
            PairType pairType = pairParams.getPairType(context);
            if (isCancelOnly || context.getHeight() % pairParams.refreshInterval == 0) {
                exchangeApi.cancelAllMyOrders(context, pairType, context.getAccount());
                continue;
            }
            Params params = context.getParams(Params.class);
            boolean shouldOptimizePrice = pairParams.shouldOptimizePrice && params.shouldOptimizePrice();
            String account = context.getAccount();
            BigDecimal marketExchangeRate = getMarketExchangeRate(context, pairType, pairParams.fixedExchangeRate);

            if (marketExchangeRate == null) {
                continue;
            }

            BigDecimal baseChainBalance = exchangeApi.getBalance(pairType.base, account);
            if (baseChainBalance == null) {
                context.logInfoMessage("Cannot load coin balance for %s, check the configuration file", pairType.getPair());
                continue;
            }
            BigDecimal counterChainBalance = exchangeApi.getBalance(pairType.counter, account);
            if (counterChainBalance == null) {
                context.logInfoMessage("Cannot load coin balance for %s, check the configuration file", pairType.getPair());
                continue;
            }

            BalanceHolder baseChainBalanceHolder = new BalanceHolder(baseChainBalance, pairParams.minBaseChainBalance);
            List<ExchangeOrderResponse> allBids = exchangeApi.getOrders(pairType, OrderType.BID);
            pairParams.bidPriceLevels
                    .forEach(priceLevel -> trade(context, pairType, OrderType.BID, priceLevel, baseChainBalanceHolder, allBids, marketExchangeRate, pairParams.shouldRandomize, shouldOptimizePrice, pairParams.ratioOfQuantityToIgnore));

            BalanceHolder counterChainBalanceHolder = new BalanceHolder(counterChainBalance, pairParams.minCounterChainBalance);
            List<ExchangeOrderResponse> allAsks = exchangeApi.getOrders(pairType, OrderType.ASK);
            pairParams.askPriceLevels
                    .forEach(priceLevel -> trade(context, pairType, OrderType.ASK, priceLevel, counterChainBalanceHolder, allAsks, marketExchangeRate, pairParams.shouldRandomize, shouldOptimizePrice, pairParams.ratioOfQuantityToIgnore));
        }
        return context.getResponse();
    }

    private BigDecimal getMarketExchangeRate(BlockContext context, PairType pairType, double fixedExchangeRate) {
        BigDecimal marketExchangeRate;
        if (fixedExchangeRate < 0.00000001) {
            marketExchangeRate = cmcApi.getExchangeRate(pairType);
            if (marketExchangeRate == null) {
                context.logInfoMessage("Cannot determine market rate for pair %s", pairType);
            }
            return marketExchangeRate;
        }
        marketExchangeRate = new BigDecimal(fixedExchangeRate);
        if (pairType.isAssetPair()) {
            marketExchangeRate = invert(marketExchangeRate);
        }
        return marketExchangeRate;
    }

    protected List<PairParams> getPairs(JO contractRunnerConfigParams) {
        return contractRunnerConfigParams.getJoList("pairs").stream().map(PairParams::new).collect(toList());
    }

    public static class PairParams {
        private final int refreshInterval;
        private final boolean shouldRandomize;
        private final double fixedExchangeRate;
        private final BigDecimal minBaseChainBalance;
        private final BigDecimal minCounterChainBalance;
        private final int ratioOfQuantityToIgnore;
        private final boolean shouldOptimizePrice;
        private final List<PriceLevel> bidPriceLevels;
        private final List<PriceLevel> askPriceLevels;
        private final PairSymbolJson fromSymbol;
        private final PairSymbolJson toSymbol;

        public PairParams(JO pairParams) {
            refreshInterval = pairParams.getInt("refreshInterval");
            JO pairSymbols = pairParams.getJo("pairSymbols");
            fromSymbol = PairSymbolJson.fromJson(pairSymbols, "from");
            toSymbol = PairSymbolJson.fromJson(pairSymbols, "to");

            shouldRandomize = pairParams.getBoolean("shouldRandomize");
            shouldOptimizePrice = pairParams.getBoolean("shouldOptimizePrice", true);
            fixedExchangeRate = pairParams.getDouble("fixedExchangeRate", 0.0);
            minBaseChainBalance = BigDecimal.valueOf(pairParams.getDouble("minBaseBalance", 0.0));
            minCounterChainBalance = BigDecimal.valueOf(pairParams.getDouble("minCounterBalance", 0.0));
            ratioOfQuantityToIgnore = pairParams.getInt("ratioOfQuantityToIgnore", 10);
            bidPriceLevels = pairParams.getJoList("bidPriceLevels")
                    .stream()
                    .map(PriceLevel::new)
                    .collect(toList());
            askPriceLevels = pairParams.getJoList("askPriceLevels")
                    .stream()
                    .map(PriceLevel::new)
                    .collect(toList());
        }

        public PairType getPairType(BlockContext context) {
            return new PairType(context, fromSymbol, toSymbol);
        }
    }

    private void trade(AbstractContractContext context, PairType pairType, OrderType orderType, PriceLevel priceLevel,
                       BalanceHolder balanceHolder, List<ExchangeOrderResponse> existingOrders, BigDecimal marketExchangeRate, boolean shouldRandomize, boolean shouldOptimizePrice, int ratioOfQuantityToIgnore) {
        byte baseChainDecimals = orderType.getBase(pairType).getDecimals();
        BigDecimal targetPrice = getTargetPrice(orderType, marketExchangeRate, priceLevel.getDiscountPercent(), baseChainDecimals);
        BigDecimal quantity = BigDecimal.valueOf(priceLevel.getQuantity());
        if (shouldRandomize) {
            quantity = BigDecimal.valueOf(randomize(quantity));
        }
        quantity = quantity.min(balanceHolder.getRemainingBalance());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            Logger.logInfoMessage("Not enough balance for %s %s %s", pairType, orderType, priceLevel);
            return;
        }
        balanceHolder.substract(quantity);
        BigDecimal orderPrice = calculateOrderPrice(pairType, orderType, priceLevel, existingOrders, shouldOptimizePrice, ratioOfQuantityToIgnore, targetPrice, quantity);
        if (orderPrice.compareTo(targetPrice) > 0) {
            // This is a protection layer that should never happen?
            // If we are better than our above our target price adjust to the target
            Logger.logInfoMessage("Setting order to target price since my price %s target price %s price level %s", orderPrice.toPlainString(), targetPrice.toPlainString(), priceLevel);
            orderPrice = targetPrice;
        }
        Logger.logInfoMessage("For pair %s my price %s to target price ratio %s coin quantity %s", pairType, orderPrice, orderPrice.divide(targetPrice, baseChainDecimals, RoundingMode.HALF_EVEN), quantity);
        exchangeApi.addOrder(context, pairType, orderType, quantity, orderPrice);
    }

    private BigDecimal calculateOrderPrice(PairType pairType, OrderType orderType, PriceLevel priceLevel, List<ExchangeOrderResponse> existingOrders, boolean shouldOptimizePrice, int ratioOfQuantityToIgnore, BigDecimal targetPrice, BigDecimal quantity) {
        if (!shouldOptimizePrice) {
            return targetPrice;
        }

        final PairSymbol base = orderType.getBase(pairType);
        final PairSymbol counter = orderType.getCounter(pairType);

        ExchangeOrderResponse bestOrder = getBestOrderVsTarget(existingOrders, targetPrice, counter.getDecimals(), quantity, ratioOfQuantityToIgnore);
        if (bestOrder == null) {
            // Otherwise just follow the price level if you just want to provide market liquidity and not compete for the price
            return targetPrice;
        }
        BigDecimal adjustment = BigDecimal.valueOf(priceLevel.getRateAdjustmentStep()).setScale(base.getDecimals(), RoundingMode.HALF_EVEN);
        // If we optimize our order we will set it's price just above the best order below it
        BigDecimal orderPrice = bestOrder.getBidPerCoin().add(adjustment).setScale(base.getDecimals(), RoundingMode.HALF_EVEN);
        Logger.logInfoMessage("Adjusting %s to %s since best price %s target price %s best quantity %s price level %s pair %s",
                orderType, orderPrice, bestOrder.getBidPerCoin(), targetPrice, bestOrder.getQuantity(), priceLevel, pairType);
        return orderPrice;
    }

    private static ExchangeOrderResponse getBestOrderVsTarget(List<ExchangeOrderResponse> orders, final BigDecimal targetPrice, byte counterDecimals, BigDecimal quantity, int ratioOfQuantityToIgnore) {
        if (orders == null) {
            Logger.logInfoMessage("No orders found");
            return null;
        }

        // Find the order the order which has the best price below our price so we can adjust our price slightly above it.
        // Ignore orders with quantity which is too small for us to care about.
        BigDecimal quantityToIgnore = quantity.setScale(counterDecimals, RoundingMode.HALF_EVEN).divide(new BigDecimal(ratioOfQuantityToIgnore), RoundingMode.HALF_EVEN);
        ExchangeOrderResponse bestOrder = orders.stream().filter(o -> {
            boolean priceFilter = o.getBidPerCoin().compareTo(targetPrice) < 0;
            return priceFilter && o.getQuantity().compareTo(quantityToIgnore) > 0;
        }).findFirst().orElse(null);
        if (bestOrder == null) {
            Logger.logInfoMessage("No existing orders better than target rate");
            return null;
        }
        return bestOrder;
    }

    private static BigDecimal getTargetPrice(OrderType orderType, BigDecimal currentPrice, double discountPercent, int decimals) {
        if (orderType.isBid()) {
            currentPrice = invert(currentPrice);
        }

        // Apply the discount percent to the existing market rate
        BigDecimal multiplier = new BigDecimal(100).subtract(new BigDecimal(discountPercent)).divide(new BigDecimal(100), decimals, RoundingMode.HALF_EVEN);
        return currentPrice.multiply(multiplier).setScale(decimals, RoundingMode.HALF_EVEN);
    }

    private static double randomize(BigDecimal value) {
        double delta = value.doubleValue() / 4;
        if (delta == 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextDouble(2 * delta) + value.doubleValue() - delta;
    }

    public static class PairType {
        private final PairSymbol base;
        private final PairSymbol counter;

        PairType(AbstractContractContext context, PairSymbolJson base, PairSymbolJson counter) {
            this.base = PairSymbol.fromJson(context, base);
            this.counter = PairSymbol.fromJson(context, counter);
            if (this.base.getHoldingType() == this.counter.getHoldingType() && this.base.getHoldingId() == this.counter.getHoldingId()) {
                throw new IllegalArgumentException("Cannot use same holding for base and counter holdings");
            }
        }

        String getPair() {
            return base.getName() + "_" + counter.getName();
        }

        String getBaseSymbol() {
            return base.getApiName();
        }

        String getCounterSymbol() {
            return counter.getApiName();
        }

        boolean isAssetPair() {
            return counter.getHoldingType() == HoldingType.ASSET;
        }

        PairSymbol getAsset() {
            return counter;
        }

        int getChainId() {
            return (int)base.getHoldingId();
        }

        PairSymbol getChain() {
            return base;
        }

        @Override
        public String toString() {
            return getPair();
        }
    }

    public enum HoldingType {
        COIN, ASSET
    }

    public static class PairSymbol {
        private final String name;
        private final String apiName;
        private final HoldingType type;
        private final long holdingId;
        private final byte decimals;

        public PairSymbol(String name, String apiName, HoldingType type, long holdingId, byte decimals) {
            this.name = name;
            this.apiName = apiName;
            this.type = type;
            this.holdingId = holdingId;
            this.decimals = decimals;
        }

        public HoldingType getHoldingType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getApiName() {
            return apiName;
        }

        public long getHoldingId() {
            return holdingId;
        }

        public byte getDecimals() {
            return decimals;
        }

        static PairSymbol fromJson(AbstractContractContext context, PairSymbolJson json) {
            if (HoldingType.COIN == json.getHoldingType()) {
                ChainWrapper chain1 = context.getChain(json.getName().toUpperCase());
                if (chain1 == null) {
                    throw new IllegalArgumentException("Undefined base chain name " + json.getName().toUpperCase());
                }
                return new PairSymbol(json.getName(), json.getApiName(), json.getHoldingType(), chain1.getId(), (byte) chain1.getDecimals());
            }

            if (HoldingType.ASSET == json.getHoldingType()) {
                final long assetId = Long.parseUnsignedLong(json.getName());
                final JO assetJson = new JO(GetAssetCall.create().asset(assetId).build().invokeNoError());
                return new PairSymbol(assetJson.getString("name"), json.getApiName(), json.getHoldingType(), assetId, assetJson.getByte("decimals"));
            }

            throw new RuntimeException("Unsupported holding type: " + json.getHoldingType());
        }
    }

    public static class CMCApi {

        private volatile Map<String, BigDecimal> quotes = Collections.emptyMap();

        private final String cmcUrl;
        private final String apiKey;
        private final String cmcFromSymbols;
        private final String cmcToSymbols;

        CMCApi(ScheduledExecutorService scheduler, String cmcUrl, String apiKey, String cmcFromSymbols, String cmcToSymbols, long refreshInterval) {
            this.cmcUrl = cmcUrl;
            this.apiKey = apiKey;
            this.cmcFromSymbols = cmcFromSymbols;
            this.cmcToSymbols = cmcToSymbols;
            scheduler.scheduleWithFixedDelay(this::loadExchangeRate, 0, refreshInterval, TimeUnit.MINUTES);
        }

        BigDecimal getExchangeRate(PairType pairType) {
            String baseSymbol = pairType.getBaseSymbol();
            String counterSymbol = pairType.getCounterSymbol();
            BigDecimal rate = quotes.get(baseSymbol + "_" + counterSymbol);
            Logger.logInfoMessage("Exchange rate between base %s and counter %s is %s", baseSymbol, counterSymbol, rate == null ? "N/A" : rate.toPlainString());
            return rate;
        }

        private void loadExchangeRate() {
            Map<String, String> parameters = new HashMap<>();
            if (cmcFromSymbols == null) {
                Logger.logInfoMessage("From symbols not specified, cannot load exchange rates");
                return;
            }
            parameters.put("symbol", cmcFromSymbols);
            if (cmcToSymbols == null) {
                Logger.logInfoMessage("To symbols not specified, cannot load exchange rates");
                return;
            }
            parameters.put("convert", cmcToSymbols);
            JO jsonResponse;
            try {
                jsonResponse = getLatestQuoteApiCall(cmcUrl, apiKey, parameters);
            } catch (Exception e) {
                Logger.logInfoMessage("Failed loading exchange rates", e);
                quotes = Collections.emptyMap(); // Never leave old quotes
                return;
            }
            System.out.println(jsonResponse.toJSONString());
            JO data = jsonResponse.getJo("data");
            String[] fromSymbols = cmcFromSymbols.split(",");
            String[] toSymbols = cmcToSymbols.split(",");
            Logger.logInfoMessage("Loading coinmarketcap rates");
            Map<String, BigDecimal> newQuotes = new HashMap<>();
            for (String fromSymbol : fromSymbols) {
                for (String toSymbol : toSymbols) {
                    if (fromSymbol.equals(toSymbol)) {
                        continue;
                    }
                    JO baseChain = data.getJo(fromSymbol);
                    JO quote = baseChain.getJo("quote");
                    JO counterChain = quote.getJo(toSymbol);
                    BigDecimal price = BigDecimal.valueOf(counterChain.getDouble("price"));
                    newQuotes.put(fromSymbol + "_" + toSymbol, price);
                    Logger.logInfoMessage("%s/%s %s", fromSymbol, toSymbol, price);
                }
            }
            quotes = Collections.unmodifiableMap(newQuotes);
            Logger.logInfoMessage("Coinmarketcap rates loaded");
        }

        private static String getParameters(Map<String, String> data) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (builder.length() > 0) {
                    builder.append("&");
                }
                try {
                    builder.append(URLEncoder.encode(entry.getKey(), "UTF8"));
                    builder.append("=");
                    builder.append(URLEncoder.encode(entry.getValue(), "UTF8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
            return builder.toString();
        }

        private static JO getLatestQuoteApiCall(String cmcUrl, String apiKey, Map<String, String> parameters) throws Exception {
            URL url = new URL(cmcUrl + "?" + getParameters(parameters));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-CMC_PRO_API_KEY", apiKey);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    return JO.parse(reader);
                }
            } else {
                throw new IllegalStateException("Error code returned from CMC: " + connection.getResponseCode());
            }

//            Using Java 11:
//            HttpClient client = HttpClient.newHttpClient();
//            String queryString = getParameters(parameters);
//            HttpRequest request = HttpRequest.newBuilder().
//                    uri(URI.create(cmcUrl + "?" + queryString)).
//                    header("Accept", "application/json").
//                    header("X-CMC_PRO_API_KEY", apiKey).
//                    GET().
//                    build();
//            HttpResponse<String> response;
//            try {
//                response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            } catch (IOException | InterruptedException e) {
//                throw new IllegalStateException(e);
//            }
//            return response.body();
        }

    }

    private static BigDecimal invert(BigDecimal value) {
        return new BigDecimal("1").setScale(8, RoundingMode.HALF_EVEN).divide(value, RoundingMode.HALF_EVEN);
    }

    public static class CoinExchangeApi implements ExchangeApi {

        @Override
        public void addOrder(AbstractContractContext context, PairType pairType, OrderType orderType, BigDecimal quantity, BigDecimal price) {
            final PairSymbol base = orderType.getBase(pairType);
            final PairSymbol counter = orderType.getCounter(pairType);
            long quantityQNT = quantity.movePointRight(counter.getDecimals()).longValue();
            long priceNQT = price.movePointRight(base.getDecimals()).longValue();
            ExchangeCoinsCall exchangeCall = ExchangeCoinsCall.create((int) base.getHoldingId())
                    .exchange((int) counter.getHoldingId())
                    .quantityQNT(quantityQNT)
                    .priceNQTPerCoin(priceNQT);
            context.createTransaction(exchangeCall);
        }

        @Override
        public void cancelAllMyOrders(AbstractContractContext context, PairType pairType, String account) {
            cancelAllMyOrders(context, pairType.base, pairType.counter, account);
            cancelAllMyOrders(context, pairType.counter, pairType.base, account);
        }

        private void cancelAllMyOrders(AbstractContractContext context, PairSymbol from, PairSymbol to, String account) {
            JO response = GetCoinExchangeOrdersCall.create((int) from.getHoldingId()).exchange((int) to.getHoldingId()).account(account).call();
            List<CoinExchangeOrderResponse> orders = response.getJoList("orders").stream().map(CoinExchangeOrderResponse::create).collect(toList());
            orders.forEach(order -> cancelOrder(context, from, order.getOrder()));
        }

        private void cancelOrder(AbstractContractContext context, PairSymbol symbol, long id) {
            context.createTransaction(CancelCoinExchangeCall.create((int) symbol.getHoldingId()).order(id));
        }

        @Override
        public List<ExchangeOrderResponse> getOrders(PairType pairType, OrderType orderType) {
            final PairSymbol base = orderType.getBase(pairType);
            final PairSymbol counter = orderType.getCounter(pairType);
            JO response = new JO(GetCoinExchangeOrdersCall
                    .create((int) base.getHoldingId())
                    .exchange((int) counter.getHoldingId())
                    .build()
                    .invokeNoError());
            return response.getJoList("orders").stream()
                    .map(CoinExchangeOrderResponse::create)
                    .map(t -> new ExchangeOrderResponse(t.getQuantityQNT(), t.getBidNQTPerCoin(), counter.getDecimals(), base.getDecimals()))
                    .collect(toList());
        }

        @Override
        public BigDecimal getBalance(PairSymbol symbol, String account) {
            JO response = GetBalanceCall.create((int) symbol.getHoldingId()).account(account).call();
            if (response.isExist("errorCode")) {
                Logger.logInfoMessage("get balance failed, response %s", response.toJSONString());
                return null;
            }
            long balanceNQT = response.getLong("unconfirmedBalanceNQT");
            return new BigDecimal(balanceNQT).movePointLeft(symbol.getDecimals());
        }
    }

    public enum OrderType {
        BID, ASK;

        public boolean isBid() {
            return this == BID;
        }

        public PairSymbol getBase(PairType pairType) {
            return isBid() ? pairType.base : pairType.counter;
        }

        public PairSymbol getCounter(PairType pairType) {
            return isBid() ? pairType.counter : pairType.base;
        }
    }

    public interface ExchangeApi {
        void addOrder(AbstractContractContext context, PairType pairType, OrderType orderType, BigDecimal quantity, BigDecimal price);

        void cancelAllMyOrders(AbstractContractContext context, PairType pairType, String account);

        List<ExchangeOrderResponse> getOrders(PairType pairType, OrderType orderType);

        BigDecimal getBalance(PairSymbol symbol, String account);
    }

    public static class AssetAskOrderResponse extends ExchangeOrderResponse {

        private AssetAskOrderResponse(PairSymbol chain, PairSymbol asset, long quantityQNT, long bidNQTPerCoin) {
            super(quantityQNT, bidNQTPerCoin, asset.getDecimals(), chain.getDecimals());
        }

        @Override
        BigDecimal getBidPerCoin() {
            return invert(getPrice());
        }

        private BigDecimal getPrice() {
            return super.getBidPerCoin();
        }
    }

    public static class ExchangeOrderResponse {
        private final long quantityQNT;
        private final long bidNQTPerCoin;
        private final byte decimalsQuantity;
        private final byte decimalsPrice;

        private ExchangeOrderResponse(long quantityQNT, long bidNQTPerCoin, byte decimalsQuantity, byte decimalsPrice) {
            this.quantityQNT = quantityQNT;
            this.bidNQTPerCoin = bidNQTPerCoin;
            this.decimalsQuantity = decimalsQuantity;
            this.decimalsPrice = decimalsPrice;
        }

        BigDecimal getQuantity() {
            return Convert.toBigDecimal(quantityQNT, decimalsQuantity);
        }

        BigDecimal getBidPerCoin() {
            return Convert.toBigDecimal(bidNQTPerCoin, decimalsPrice);
        }
    }

    public static class SelectExchangeApi implements ExchangeApi {
        private final ExchangeApi coinExchange;
        private final ExchangeApi assetExchange;

        public SelectExchangeApi(ExchangeApi coinExchange, ExchangeApi assetExchange) {
            this.coinExchange = coinExchange;
            this.assetExchange = assetExchange;
        }

        private ExchangeApi getExchangeApi(PairType pairType) {
            final PairSymbol base = pairType.base;
            final PairSymbol counter = pairType.counter;
            if (base.getHoldingType() == HoldingType.COIN && counter.getHoldingType() == HoldingType.COIN) {
                return coinExchange;
            }
            if ((pairType.getAsset().getHoldingType() == HoldingType.ASSET
                    && pairType.getChain().getHoldingType() == HoldingType.COIN)) {
                return assetExchange;
            }
            throw new RuntimeException("Unsupported pair type holdings combination: "
                    + base.getHoldingType() + ", " + counter.getHoldingType());
        }

        public ExchangeApi getExchangeApi(PairSymbol symbol) {
            if (symbol.getHoldingType() == HoldingType.COIN) {
                return coinExchange;
            }

            if (symbol.getHoldingType() == HoldingType.ASSET) {
                return assetExchange;
            }

            throw new RuntimeException("Unsupported pair symbol: " + symbol.getHoldingType());
        }

        @Override
        public void addOrder(AbstractContractContext context, PairType pairType, OrderType orderType, BigDecimal quantity, BigDecimal price) {
            getExchangeApi(pairType).addOrder(context, pairType, orderType, quantity, price);
        }

        @Override
        public void cancelAllMyOrders(AbstractContractContext context, PairType pairType, String account) {
            getExchangeApi(pairType).cancelAllMyOrders(context, pairType, account);
        }

        @Override
        public List<ExchangeOrderResponse> getOrders(PairType pairType, OrderType orderType) {
            return getExchangeApi(pairType).getOrders(pairType, orderType);
        }

        @Override
        public BigDecimal getBalance(PairSymbol symbol, String account) {
            return getExchangeApi(symbol).getBalance(symbol, account);
        }
    }

    public static class PriceLevel {
        private final double discountPercent;
        private final double quantity;
        private final double rateAdjustmentStep;

        PriceLevel(JO priceLevelJson) {
            discountPercent = priceLevelJson.getDouble("discountPercent");
            quantity = priceLevelJson.getDouble("quantity");
            rateAdjustmentStep = priceLevelJson.getDouble("rateAdjustmentStep");
        }

        double getDiscountPercent() {
            return discountPercent;
        }

        double getQuantity() {
            return quantity;
        }

        double getRateAdjustmentStep() {
            return rateAdjustmentStep;
        }

        @Override
        public String toString() {
            return "PriceLevel{" +
                    "discountPercent=" + discountPercent +
                    ", quantity=" + quantity +
                    ", rateAdjustmentStep=" + rateAdjustmentStep +
                    '}';
        }
    }

    public static class BalanceHolder {
        private BigDecimal balance;
        private final BigDecimal minBalance;

        BalanceHolder(BigDecimal balance, BigDecimal minBalance) {
            this.balance = balance;
            this.minBalance = minBalance.setScale(balance.scale(), RoundingMode.HALF_EVEN);
        }

        void substract(BigDecimal value) {
            balance = balance.subtract(value);
        }

        BigDecimal getRemainingBalance() {
            return balance.subtract(minBalance);
        }
    }

    public static class PairSymbolJson {
        private final String name;
        private final String apiName;
        private final HoldingType holdingType;

        public PairSymbolJson(String name, String apiName, String holdingType) {
            this.name = name;
            if (apiName != null) {
                this.apiName = apiName;
            } else {
                this.apiName = name;
            }
            this.holdingType = HoldingType.valueOf(holdingType);
        }

        public HoldingType getHoldingType() {
            return holdingType;
        }

        public String getName() {
            return name;
        }

        public String getApiName() {
            return apiName;
        }

        public static PairSymbolJson fromJson(JO jo, String side) {
            return new PairSymbolJson(jo.getString(side), jo.getString(side + "Symbol"), jo.getString(side + "Holding"));
        }
    }

    public static class AssetExchangeApi implements ExchangeApi {
        @Override
        public void addOrder(AbstractContractContext context, PairType pairType, OrderType orderType, BigDecimal quantity, BigDecimal price) {
            final int chainId = pairType.getChainId();
            final PairSymbol chain = pairType.getChain();
            final PairSymbol asset = pairType.getAsset();
            final APICall.Builder<?> apiCall;
            long quantityQNT = quantity.movePointRight(asset.getDecimals()).longValue();
            if (orderType.isBid()) {
                long priceNQT = price.movePointRight(chain.getDecimals()).longValue();
                apiCall = PlaceBidOrderCall.create(chainId)
                        .asset(asset.getHoldingId())
                        .priceNQTPerShare(priceNQT)
                        .quantityQNT(quantityQNT);
            } else {
                final BigDecimal assetPrice = invert(price);
                long priceNQT = assetPrice.movePointRight(chain.getDecimals()).longValue();
                apiCall = PlaceAskOrderCall.create(chainId)
                        .asset(asset.getHoldingId())
                        .priceNQTPerShare(priceNQT)
                        .quantityQNT(quantityQNT);
            }
            context.createTransaction(apiCall);
        }

        @Override
        public void cancelAllMyOrders(AbstractContractContext context, PairType pairType, String account) {
            final int chainId = pairType.getChainId();
            final long assetId = pairType.getAsset().getHoldingId();
            final JSONObject bidsJson = GetAccountCurrentBidOrdersCall.create(chainId)
                    .account(account)
                    .asset(assetId)
                    .firstIndex(0)
                    .lastIndex(-1)
                    .build().invokeNoError();
            ((List<?>) bidsJson.get("bidOrders"))
                    .stream()
                    .map(JSONObject.class::cast)
                    .map(o -> o.get("order"))
                    .map(String.class::cast)
                    .forEach(orderId -> context.createTransaction(CancelBidOrderCall.create(chainId).order(orderId)));

            final JSONObject asksJson = GetAccountCurrentAskOrdersCall.create(chainId)
                    .account(account)
                    .asset(assetId)
                    .firstIndex(0)
                    .lastIndex(-1)
                    .build().invokeNoError();
            ((List<?>) asksJson.get("askOrders"))
                    .stream()
                    .map(JSONObject.class::cast)
                    .map(o -> o.get("order"))
                    .map(String.class::cast)
                    .forEach(orderId -> context.createTransaction(CancelAskOrderCall.create(chainId).order(orderId)));
        }

        @Override
        public List<ExchangeOrderResponse> getOrders(PairType pairType, OrderType orderType) {
            final PairSymbol chain = pairType.getChain();
            final PairSymbol asset = pairType.getAsset();

            final List<?> jsonObject = (List<?>) (orderType.isBid()
                    ? GetBidOrdersCall.create((int) chain.getHoldingId())
                    .asset(asset.getHoldingId())
                    .firstIndex(0).lastIndex(-1)
                    .build().invokeNoError().get("bidOrders")
                    : GetAskOrdersCall.create((int) chain.getHoldingId())
                    .asset(asset.getHoldingId())
                    .firstIndex(0).lastIndex(-1)
                    .build().invokeNoError().get("askOrders"));
            final BiFunction<Long, Long, ExchangeOrderResponse> function = orderType.isBid()
                    ? (quantityQNT1, bidNQTPerCoin1) -> new ExchangeOrderResponse(quantityQNT1, bidNQTPerCoin1, asset.getDecimals(), chain.getDecimals())
                    : (quantityQNT, bidNQTPerCoin) -> new AssetAskOrderResponse(chain, asset, quantityQNT, bidNQTPerCoin);

            return jsonObject.stream()
                    .map(JO::new)
                    .map(o -> function.apply(Long.parseLong(o.getString("quantityQNT")), Long.parseLong(o.getString("priceNQTPerShare"))))
                    .collect(toList());
        }

        @Override
        public BigDecimal getBalance(PairSymbol symbol, String account) {
            final JSONObject json = GetAccountCall.create().account(account).includeAssets(true).build().invokeNoError();
            final long assetId = symbol.getHoldingId();
            return ((List<?>) json.getOrDefault("assetBalances", emptyList())).stream()
                    .map(JO::new)
                    .filter(o -> assetId == Long.parseUnsignedLong(o.getString("asset")))
                    .map(o -> o.getString("balanceQNT"))
                    .map(s -> new BigDecimal(s).setScale(symbol.getDecimals(), RoundingMode.HALF_EVEN))
                    .findFirst()
                    .orElse(new BigDecimal(0L));
        }
    }
}
