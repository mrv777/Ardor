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

public class AssetExchangeTradeResponseImpl implements AssetExchangeTradeResponse {

    private final int timeStamp;
    private final long quantityQNT;
    private final BigDecimal quantity;
    private final long priceNQTPerCoin;
    private final BigDecimal pricePerCoin;
    private final long asset;
    private final byte[] askOrderFullHash;
    private final byte[] bidOrderFullHash;
    private final int askOrderHeight;
    private final int bidOrderHeight;
    private final long seller;
    private final long buyer;
    private final long block;
    private final int height;
    private final boolean isBuy;
    private final AssetEntityResponse assetInfo;

    AssetExchangeTradeResponseImpl(JSONObject response) {
        this(new JO(response));
    }

    AssetExchangeTradeResponseImpl(JO tradeJson) {
        timeStamp = tradeJson.getInt("timestamp");
        quantityQNT = tradeJson.getLong("quantityQNT");
        priceNQTPerCoin = tradeJson.getLong("priceNQTPerShare");
        asset = tradeJson.getEntityId("asset");
        askOrderFullHash = tradeJson.parseHexString("askOrderFullHash");
        bidOrderFullHash = tradeJson.parseHexString("bidOrderFullHash");
        askOrderHeight = tradeJson.getInt("askOrderHeight");
        bidOrderHeight = tradeJson.getInt("bidOrderHeight");
        seller = tradeJson.getEntityId("seller");
        buyer = tradeJson.getEntityId("buyer");
        block = tradeJson.getEntityId("block");
        height = tradeJson.getInt("height");
        isBuy = "buy".equals(tradeJson.getString("tradeType"));
        if (tradeJson.isExist("decimals")) {
            assetInfo = AssetEntityResponse.create(tradeJson);
            quantity = Convert.toBigDecimal(quantityQNT, assetInfo.getDecimals());
            pricePerCoin = Convert.toBigDecimal(priceNQTPerCoin, assetInfo.getDecimals());
        } else {
            assetInfo = null;
            quantity = null;
            pricePerCoin = null;
        }
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }
    
    public BigDecimal getQuantity() {
        if (assetInfo == null) {
            throw new IllegalStateException("Unknown asset decimals");
        }
        return quantity;
    }

    public long getPriceNQTPerCoin() {
        return priceNQTPerCoin;
    }

    public BigDecimal getPricePerCoin() {
        if (assetInfo == null) {
            throw new IllegalStateException("Unknown asset decimals");
        }
        return pricePerCoin;
    }

    public long getAsset() {
        return asset;
    }

    public byte[] getAskOrderFullHash() {
        return askOrderFullHash;
    }

    public byte[] getBidOrderFullHash() {
        return bidOrderFullHash;
    }

    public int getAskOrderHeight() {
        return askOrderHeight;
    }

    public int getBidOrderHeight() {
        return bidOrderHeight;
    }

    public long getSeller() {
        return seller;
    }

    public long getBuyer() {
        return buyer;
    }

    public long getBlock() {
        return block;
    }

    public int getHeight() {
        return height;
    }

    public boolean isBuy() {
        return isBuy;
    }

    public AssetEntityResponse getAssetInfo() {
        return assetInfo;
    }
}