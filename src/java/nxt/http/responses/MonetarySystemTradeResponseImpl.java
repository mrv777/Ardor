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

public class MonetarySystemTradeResponseImpl implements MonetarySystemTradeResponse {

    private final byte[] transactionFullHash;
    private final int timestamp;
    private final long unitsQNT;
    private final BigDecimal units;
    private final long rateNQTPerUnit;
    private final BigDecimal ratePerUnit;
    private final long currency;
    private final long offer;
    private final byte[] offerFullHash;
    private final long seller;
    private final long buyer;
    private final long block;
    private final int height;
    private final CurrencyEntityResponse currencyInfo;

    MonetarySystemTradeResponseImpl(JSONObject response) {
        this(new JO(response));
    }

    MonetarySystemTradeResponseImpl(JO tradeJson) {
        transactionFullHash = tradeJson.parseHexString("transactionFullHash");
        timestamp = tradeJson.getInt("timestamp");
        unitsQNT = tradeJson.getLong("unitsQNT");
        rateNQTPerUnit = tradeJson.getLong("rateNQTPerUnit");
        currency = tradeJson.getEntityId("currency");
        offer = tradeJson.getEntityId("offer");
        offerFullHash = tradeJson.parseHexString("offerFullHash");
        seller = tradeJson.getEntityId("seller");
        buyer = tradeJson.getEntityId("buyer");
        block = tradeJson.getEntityId("block");
        height = tradeJson.getInt("height");
        if (tradeJson.isExist("decimals")) {
            currencyInfo = CurrencyEntityResponse.create(tradeJson);
            units = Convert.toBigDecimal(unitsQNT, currencyInfo.getDecimals());
            ratePerUnit = Convert.toBigDecimal(rateNQTPerUnit, currencyInfo.getDecimals());
        } else {
            currencyInfo = null;
            units = null;
            ratePerUnit = null;
        }
    }

    @Override
    public byte[] getTransactionFullHash() {
        return transactionFullHash;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getUnitsQNT() {
        return unitsQNT;
    }

    @Override
    public BigDecimal getUnits() {
        return units;
    }

    @Override
    public long getRateNQTPerUnit() {
        return rateNQTPerUnit;
    }

    @Override
    public BigDecimal getRatePerUnit() {
        return ratePerUnit;
    }

    @Override
    public long getCurrency() {
        return currency;
    }

    @Override
    public long getOffer() {
        return offer;
    }

    @Override
    public byte[] getOfferFullHash() {
        return offerFullHash;
    }

    @Override
    public long getSeller() {
        return seller;
    }

    @Override
    public long getBuyer() {
        return buyer;
    }

    @Override
    public long getBlock() {
        return block;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public CurrencyEntityResponse getCurrencyInfo() {
        return currencyInfo;
    }
}