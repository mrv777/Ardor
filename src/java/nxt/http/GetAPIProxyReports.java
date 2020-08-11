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
package nxt.http;

import nxt.NxtException;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static nxt.http.JSONResponses.ERROR_DISABLED;

public class GetAPIProxyReports extends APIServlet.APIRequestHandler {

    static final GetAPIProxyReports instance = new GetAPIProxyReports();

    private GetAPIProxyReports() {
        super(new APITag[] {APITag.INFO, APITag.NETWORK});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        if (!APIProxy.enableAPIProxy) {
            return ERROR_DISABLED;
        }
        JSONArray reports = APIProxy.getInstance().getReportsManager().getConfirmationReports().stream()
                .map(JSONData::confirmationReport)
                .collect(JSON.jsonArrayCollector());

        Collections.reverse(reports);

        return new JSONObject(Collections.singletonMap("reports", reports));
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
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }
}
