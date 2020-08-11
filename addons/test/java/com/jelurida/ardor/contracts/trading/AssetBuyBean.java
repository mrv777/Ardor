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

import nxt.BlockchainTest;
import nxt.http.callers.PlaceBidOrderCall;
import org.json.simple.JSONObject;

import static nxt.blockchain.ChildChain.IGNIS;

class AssetBuyBean extends AssetOrderBean {
    public AssetBuyBean(long price, long quantity, long asset) {
        super(price, quantity, asset);
    }

    public static AssetBuyBean fromJSONObject(JSONObject o) {
        long asset = Long.parseUnsignedLong((String) o.get("asset"));
        long quantityQNT = Long.parseLong((String) o.get("quantityQNT"));
        long price = Long.parseLong((String) o.get("priceNQTPerShare"));
        return new AssetBuyBean(price, quantityQNT, asset);
    }

    @Override
    void placeOrder() {
        PlaceBidOrderCall.create(IGNIS.getId())
                .quantityQNT(quantity)
                .priceNQTPerShare(price)
                .asset(asset)
                .secretPhrase(BlockchainTest.DAVE.getSecretPhrase())
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();
    }

}
