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
import nxt.ms.CurrencyType;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.math.BigDecimal;

public class CurrencyEntityResponseImpl implements CurrencyEntityResponse {

    private final long currency;
    private final long account;
    private final String name;
    private final String code;
    private final String description;
    private final int type;
    private final CurrencyType currencyType;
    private final int chain;
    private final byte decimals;
    private final long initialSupplyQNT;
    private final BigDecimal initialSupply;
    private final long currentSupplyQNT;
    private final BigDecimal currentSupply;
    private final long reserveSupplyQNT;
    private final BigDecimal reserveSupply;
    private final long maxSupplyQNT;
    private final BigDecimal maxSupply;
    private final int creationHeight;
    private final int issuanceHeight;
    private final long minReservePerUnitNQT;
    private final BigDecimal minReservePerUnit;
    private final long currentReservePerUnitNQT;
    private final BigDecimal currentReservePerUnit;
    private final int minDifficulty;
    private final int maxDifficulty;
    private final byte algorithm;
    private final int numberOfTransfers;

    CurrencyEntityResponseImpl(JSONObject response) {
        this(new JO(response));
    }

    CurrencyEntityResponseImpl(JO currencyJson) {
        currency = currencyJson.getEntityId("currency");
        account = currencyJson.getEntityId("account");
        name = currencyJson.getString("name");
        code = currencyJson.getString("code");
        description = currencyJson.getString("description");
        type = currencyJson.getInt("type");
        currencyType = CurrencyType.get(type);
        chain = currencyJson.getInt("chain");
        decimals = currencyJson.getByte("decimals");
        initialSupplyQNT = currencyJson.getLong("initialSupplyQNT");
        initialSupply = Convert.toBigDecimal(initialSupplyQNT, decimals);
        currentSupplyQNT = currencyJson.getLong("currentSupplyQNT");
        currentSupply = Convert.toBigDecimal(currentSupplyQNT, decimals);
        reserveSupplyQNT = currencyJson.getLong("reserveSupplyQNT");
        reserveSupply = Convert.toBigDecimal(reserveSupplyQNT, decimals);
        maxSupplyQNT = currencyJson.getLong("maxSupplyQNT");
        maxSupply = Convert.toBigDecimal(maxSupplyQNT, decimals);
        creationHeight = currencyJson.getInt("creationHeight");
        issuanceHeight = currencyJson.getInt("issuanceHeight");
        minReservePerUnitNQT = currencyJson.getLong("minReservePerUnitNQT");
        minReservePerUnit = Convert.toBigDecimal(minReservePerUnitNQT, decimals);
        currentReservePerUnitNQT = currencyJson.getLong("currentReservePerUnitNQT");
        currentReservePerUnit = Convert.toBigDecimal(currentReservePerUnitNQT, decimals);
        minDifficulty = currencyJson.getInt("minDifficulty");
        maxDifficulty = currencyJson.getInt("maxDifficulty");
        algorithm = currencyJson.getByte("algorithm");
        numberOfTransfers = currencyJson.getInt("numberOfTransfers");
    }

    @Override
    public long getCurrency() {
        return currency;
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
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    @Override
    public int getChain() {
        return chain;
    }

    @Override
    public byte getDecimals() {
        return decimals;
    }

    @Override
    public long getInitialSupplyQNT() {
        return initialSupplyQNT;
    }

    @Override
    public BigDecimal getInitialSupply() {
        return initialSupply;
    }

    @Override
    public long getCurrentSupplyQNT() {
        return currentSupplyQNT;
    }

    @Override
    public BigDecimal getCurrentSupply() {
        return currentSupply;
    }

    @Override
    public long getReserveSupplyQNT() {
        return reserveSupplyQNT;
    }

    @Override
    public BigDecimal getReserveSupply() {
        return reserveSupply;
    }

    @Override
    public long getMaxSupplyQNT() {
        return maxSupplyQNT;
    }

    @Override
    public BigDecimal getMaxSupply() {
        return maxSupply;
    }

    @Override
    public int getCreationHeight() {
        return creationHeight;
    }

    @Override
    public int getIssuanceHeight() {
        return issuanceHeight;
    }

    @Override
    public long getMinReservePerUnitNQT() {
        return minReservePerUnitNQT;
    }

    @Override
    public BigDecimal getMinReservePerUnit() {
        return minReservePerUnit;
    }

    @Override
    public long getCurrentReservePerUnitNQT() {
        return currentReservePerUnitNQT;
    }

    @Override
    public BigDecimal getCurrentReservePerUnit() {
        return currentReservePerUnit;
    }

    @Override
    public int getMinDifficulty() {
        return minDifficulty;
    }

    @Override
    public int getMaxDifficulty() {
        return maxDifficulty;
    }

    @Override
    public byte getAlgorithm() {
        return algorithm;
    }

    @Override
    public int getNumberOfTransfers() {
        return numberOfTransfers;
    }
}
