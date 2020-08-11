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

package nxt.http.responses;

import nxt.addons.JO;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.math.BigDecimal;

public class CoinExchangeOrderResponseImpl implements CoinExchangeOrderResponse {

    private final long order;
    private final byte[] orderFullHash;
    private final int chain;
    private final int exchangeChain;
    private final long account;
    private final long quantityQNT;
    private final long exchangeQNT;
    private final long bidNQTPerCoin;
    private final long askNQTPerCoin;

    CoinExchangeOrderResponseImpl(JSONObject response) {
        this(new JO(response));
    }

    CoinExchangeOrderResponseImpl(JO tradeJson) {
        order = tradeJson.getEntityId("order");
        orderFullHash = tradeJson.parseHexString("orderFullHash");
        chain = tradeJson.getInt("chain");
        exchangeChain = tradeJson.getInt("exchange");
        account = tradeJson.getEntityId("account");
        quantityQNT = tradeJson.getLong("quantityQNT");
        exchangeQNT = tradeJson.getLong("exchangeQNT");
        bidNQTPerCoin = tradeJson.getLong("bidNQTPerCoin");
        askNQTPerCoin = tradeJson.getLong("askNQTPerCoin");
    }

    @Override
    public long getOrder() {
        return order;
    }

    @Override
    public byte[] getOrderFullHash() {
        return orderFullHash;
    }

    @Override
    public int getChain() {
        return chain;
    }

    @Override
    public int getExchangeChain() {
        return exchangeChain;
    }

    @Override
    public long getAccount() {
        return account;
    }

    @Override
    public long getQuantityQNT() {
        return quantityQNT;
    }

    @Override
    public long getExchangeQNT() {
        return exchangeQNT;
    }

    @Override
    public long getBidNQTPerCoin() {
        return bidNQTPerCoin;
    }

    @Override
    public long getAskNQTPerCoin() {
        return askNQTPerCoin;
    }

    @Override
    public BigDecimal getQuantity(byte decimals) {
        return Convert.toBigDecimal(quantityQNT, decimals);
    }

    @Override
    public BigDecimal getExchangeQuantity(byte decimals) {
        return Convert.toBigDecimal(exchangeQNT, decimals);
    }

    @Override
    public BigDecimal getBidPerCoin(byte decimals) {
        return Convert.toBigDecimal(bidNQTPerCoin, decimals);
    }

    @Override
    public BigDecimal getAskPerCoin(byte decimals) {
        return Convert.toBigDecimal(askNQTPerCoin, decimals);
    }
}