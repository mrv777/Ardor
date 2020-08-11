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

package nxt.http;

import nxt.addons.AddOn;
import nxt.http.APIServlet.APIRequestHandler;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static nxt.http.APITag.ADDONS;

public class CustomSensitiveParameterAddOn implements AddOn {
    static final String parameterName = "customSensitiveParameter";
    static final String handlerName = "customSensitiveParameterAddOnRequestHandler";

    @Override
    public Map<String, APIRequestHandler> getAPIRequests() {
        return Collections.singletonMap(handlerName, new RequestHandler());
    }

    private static class RequestHandler extends APIRequestHandler {

        protected RequestHandler() {
            super(new APITag[]{ADDONS}, parameterName);
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) {
            return new JSONObject();
        }

        @Override
        protected List<String> getSensitiveParameters() {
            return singletonList(parameterName);
        }
    }
}
