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

import nxt.BlockchainTest;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.blockchain.TransactionType;
import nxt.http.callers.GetConstantsCall;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.junit.Test;

import static nxt.util.JSON.jsonArrayCollector;
import static org.junit.Assert.assertEquals;

public class GetConstantsTest extends BlockchainTest {

    @Test
    public void testGetConstantsChainProperties() {
        JSONObject actual = GetConstantsCall.create().build().invokeNoError();
        JSONObject chainProperties = (JSONObject) actual.get("chainProperties");
        assertEquals(expectedChainProperties(FxtChain.FXT), chainProperties.get("1"));
        assertEquals(expectedChildChainProperties(ChildChain.IGNIS), chainProperties.get("2"));
        assertEquals(expectedChildChainProperties(ChildChain.AEUR), chainProperties.get("3"));
        assertEquals(expectedChildChainProperties(ChildChain.BITSWIFT), chainProperties.get("4"));
        assertEquals(expectedChildChainProperties(ChildChain.MPG), chainProperties.get("5"));
    }

    private JSONObject expectedChainProperties(Chain chain) {
        JSONObject result = new JSONObject();
        result.put("totalAmount", String.valueOf(chain.getTotalAmount()));
        result.put("ONE_COIN", String.valueOf(chain.ONE_COIN));
        result.put("decimals", (long) chain.getDecimals());
        result.put("name", chain.getName());
        result.put("id", (long) chain.getId());
        result.put("disabledTransactionTypes", chain.getDisabledTransactionTypes().stream()
                .map(TransactionType::getName)
                .collect(jsonArrayCollector()));
        result.put("disabledAPITags", chain.getDisabledAPITags().stream()
                .map(APITag::name)
                .collect(jsonArrayCollector()));
        return result;
    }

    private JSONObject expectedChildChainProperties(ChildChain chain) {
        JSONObject result = expectedChainProperties(chain);
        result.put("SHUFFLING_DEPOSIT_NQT", String.valueOf(chain.SHUFFLING_DEPOSIT_NQT));
        result.put("permissionPolicy", chain.getPermissionPolicy().getName());
        result.put("masterAdmins", chain.getMasterAdminAccounts().stream().map(Convert::rsAccount).collect(jsonArrayCollector()));

        return result;
    }
}