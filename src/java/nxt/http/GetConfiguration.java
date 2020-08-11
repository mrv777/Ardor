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

import nxt.NxtException;
import nxt.configuration.ConfigParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetConfiguration extends APIServlet.APIRequestHandler {

    static final GetConfiguration instance = new GetConfiguration();

    private GetConfiguration() {
        super(new APITag[]{APITag.UTILS});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        ConfigParser configParser = ConfigParser.getInstance();

        JSONObject response = new JSONObject();
        JSONArray properties = new JSONArray();
        configParser.getProperties().values().stream().map(JSONData::configProperty).forEach(properties::add);
        response.put("properties", properties);
        return response;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected boolean isChainSpecific() {
        return false;
    }
}
