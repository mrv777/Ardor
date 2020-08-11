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
import nxt.blockchain.ChildChain;
import nxt.blockchain.chaincontrol.ChildChainPermission;
import nxt.blockchain.chaincontrol.PermissionType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class GetChainPermissions extends APIServlet.APIRequestHandler {
    static final GetChainPermissions instance = new GetChainPermissions();

    private GetChainPermissions() {
        super(new APITag[]{APITag.CHILD_CHAIN_CONTROL}, "permission", "granter", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        ChildChain chain = ParameterParser.getChildChain(req);
        long granterId = ParameterParser.getAccountId(req, "granter", false);
        PermissionType permissionType = ParameterParser.getPermissionName(req, false);

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();

        List<ChildChainPermission> permissions = chain.getPermissionReader().getPermissions(permissionType, granterId, -1, firstIndex, lastIndex);
        Collector<JSONObject, ?, JSONArray> collector = Collectors.toCollection(JSONArray::new);
        JSONArray jsonArray = permissions.stream().map(JSONData::permission).collect(collector);
        response.put("permissions", jsonArray);
        return response;
    }

}
