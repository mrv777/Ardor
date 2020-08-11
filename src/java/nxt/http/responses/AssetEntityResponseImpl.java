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

package nxt.http.responses;

import nxt.addons.JO;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AssetEntityResponseImpl implements AssetEntityResponse {

    private final long account;
    private final String name;
    private final String description;
    private final byte decimals;
    private final long quantityQNT;
    private final BigDecimal quantity;
    private final long asset;
    private final boolean hasPhasingAssetControl;
    private final int numberOfTransfers;
    private final int numberOfAccounts;

    AssetEntityResponseImpl(JSONObject response) {
        this(new JO(response));
    }

    AssetEntityResponseImpl(JO assetJson) {
        account = assetJson.getEntityId("account");
        name = assetJson.getString("name");
        description = assetJson.getString("description");
        decimals = assetJson.getByte("decimals");
        quantityQNT = assetJson.getLong("quantityQNT");
        quantity = new BigDecimal(BigInteger.valueOf(quantityQNT), decimals);
        asset = assetJson.getEntityId("asset");
        hasPhasingAssetControl = assetJson.getBoolean("hasPhasingAssetControl");
        if (assetJson.isExist("numberOfTransfers")) {
            numberOfTransfers = assetJson.getInt("numberOfTransfers");
        } else {
            numberOfTransfers = 0;
        }
        if (assetJson.isExist("numberOfTransfers")) {
            numberOfAccounts = assetJson.getInt("numberOfAccounts");
        } else {
            numberOfAccounts = 0;
        }
    }

    @Override
    public long getAccount() {
        return account;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public byte getDecimals() {
        return decimals;
    }

    @Override
    public long getQuantityQNT() {
        return quantityQNT;
    }

    @Override
    public BigDecimal getQuantity() {
        return quantity;
    }

    @Override
    public long getAsset() {
        return asset;
    }

    @Override
    public boolean isHasPhasingAssetControl() {
        return hasPhasingAssetControl;
    }

    @Override
    public int getNumberOfTransfers() {
        return numberOfTransfers;
    }

    @Override
    public int getNumberOfAccounts() {
        return numberOfAccounts;
    }
}
