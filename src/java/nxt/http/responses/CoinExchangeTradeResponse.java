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
import org.json.simple.JSONObject;

import java.math.BigDecimal;

public interface CoinExchangeTradeResponse {

    static CoinExchangeTradeResponse create(JO object) {
        return new CoinExchangeTradeResponseImpl(object);
    }

    static CoinExchangeTradeResponse create(JSONObject object) {
        return new CoinExchangeTradeResponseImpl(object);
    }

    byte[] getOrderFullHash();

    byte[] getMatchFullHash();

    int getChainId();

    int getExchangeChainId();

    long getAccountId();

    String getAccount();

    String getAccountRs();

    long getQuantityQNT();

    BigDecimal getQuantity(byte qntDecimals);

    long getPriceNQTPerCoin();

    BigDecimal getPricePerCoin(byte nqtDecimals);

    double getExchangeRate();

    long getBlockId();

    int getHeight();

    int getTimestamp();
}
