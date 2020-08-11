/*
 * Copyright © 2013-2016 The Nxt Core Developers.
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

package nxt.http;

import nxt.Constants;
import nxt.NxtException;
import nxt.peer.BundlerRate;
import nxt.peer.FeeRateCalculator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.peer.FeeRateCalculator.TransactionPriority.NORMAL;

public final class GetBundlerRates extends APIServlet.APIRequestHandler {

    static final GetBundlerRates instance = new GetBundlerRates();

    private GetBundlerRates() {
        super(new APITag[]{APITag.FORGING}, "minBundlerBalanceFXT", "minBundlerFeeLimitFQT",
                "transactionPriority");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        FeeRateCalculator feeRateCalculator = FeeRateCalculator.create()
                .setMinBalance(ParameterParser.getLong(req, "minBundlerBalanceFXT", 0, Constants.MAX_BALANCE_FXT, Constants.minBundlerBalanceFXT))
                .setMinFeeLimit(ParameterParser.getLong(req, "minBundlerFeeLimitFQT", 0, Constants.MAX_BALANCE_FXT * Constants.ONE_FXT, Constants.minBundlerFeeLimitFXT * Constants.ONE_FXT))
                .setPriority(ParameterParser.getPriority(req, "transactionPriority", NORMAL))
                .build();
        List<BundlerRate> rates = feeRateCalculator.getBestRates();

        JSONObject response = new JSONObject();
        JSONArray ratesJSON = new JSONArray();
        rates.forEach(rate -> {
            JSONObject rateJSON = new JSONObject();
            rateJSON.put("chain", rate.getChain().getId());
            JSONData.putAccount(rateJSON, "account", rate.getAccountId());
            rateJSON.put("minRateNQTPerFXT", String.valueOf(rate.getRate()));
            rateJSON.put("currentFeeLimitFQT", String.valueOf(rate.getFeeLimit()));
            ratesJSON.add(rateJSON);
        });
        response.put("rates", ratesJSON);
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean isChainSpecific() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }
}
