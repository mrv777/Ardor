package com.jelurida.ardor.contracts.trading;

import nxt.BlockchainTest;
import nxt.http.callers.PlaceAskOrderCall;
import org.json.simple.JSONObject;

import static nxt.blockchain.ChildChain.IGNIS;

class AssetSellBean extends AssetOrderBean {
    public AssetSellBean(long price, long quantity, long asset) {
        super(price, quantity, asset);
    }

    @Override
    void placeOrder() {
        PlaceAskOrderCall.create(IGNIS.getId())
                .quantityQNT(quantity)
                .priceNQTPerShare(price)
                .asset(asset)
                .secretPhrase(BlockchainTest.DAVE.getSecretPhrase())
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();
    }

    public static AssetSellBean fromJSONObject(JSONObject o) {
        long asset = Long.parseUnsignedLong((String) o.get("asset"));
        long quantityQNT = Long.parseLong((String) o.get("quantityQNT"));
        long price = Long.parseLong((String) o.get("priceNQTPerShare"));
        return new AssetSellBean(price, quantityQNT, asset);
    }
}
