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
import org.json.simple.JSONObject;

import java.math.BigDecimal;

public interface CurrencyEntityResponse {

    static CurrencyEntityResponse create(JO object) {
        return new CurrencyEntityResponseImpl(object);
    }

    static CurrencyEntityResponse create(JSONObject object) {
        return new CurrencyEntityResponseImpl(object);
    }

    long getCurrency();

    long getAccount();

    String getName();

    String getCode();

    String getDescription();

    int getType();

    CurrencyType getCurrencyType();

    int getChain();

    byte getDecimals();

    long getInitialSupplyQNT();

    BigDecimal getInitialSupply();

    long getCurrentSupplyQNT();

    BigDecimal getCurrentSupply();

    long getReserveSupplyQNT();

    BigDecimal getReserveSupply();

    long getMaxSupplyQNT();

    BigDecimal getMaxSupply();

    int getCreationHeight();

    int getIssuanceHeight();

    long getMinReservePerUnitNQT();

    BigDecimal getMinReservePerUnit();

    long getCurrentReservePerUnitNQT();

    BigDecimal getCurrentReservePerUnit();

    int getMinDifficulty();

    int getMaxDifficulty();

    byte getAlgorithm();

    int getNumberOfTransfers();
}
