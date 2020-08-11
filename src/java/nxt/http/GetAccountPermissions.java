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
import nxt.blockchain.chaincontrol.PermissionReader.ChildChainAccountPermissions;
import nxt.blockchain.chaincontrol.PermissionType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

import static nxt.util.JSON.jsonArrayCollector;

public final class GetAccountPermissions extends APIServlet.APIRequestHandler {
    static final GetAccountPermissions instance = new GetAccountPermissions();

    private GetAccountPermissions() {
        super(new APITag[]{APITag.CHILD_CHAIN_CONTROL}, "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long accountId = ParameterParser.getAccountId(req, true);
        ChildChain childChain = ParameterParser.getChildChain(req);

        ChildChainAccountPermissions permissions = childChain.getPermissionReader().getPermissions(accountId);
        List<ChildChainPermission> effective = permissions.getEffectivePermissions();
        JSONArray canGrantPermissions = effective.stream()
                .map(ChildChainPermission::getPermissionType)
                .map(PermissionType::canGrant)
                .flatMap(Collection::stream)
                .map(PermissionType::name)
                .collect(jsonArrayCollector());
        JSONObject result = new JSONObject();
        result.put("hasPermissions", asJson(permissions.getPermissions()));
        result.put("hasEffectivePermissions", asJson(effective));
        result.put("canGrantPermissions", canGrantPermissions);
        return result;
    }

    private static JSONArray asJson(List<ChildChainPermission> permissions) {
        return permissions.stream().map(JSONData::permission).collect(jsonArrayCollector());
    }
}
