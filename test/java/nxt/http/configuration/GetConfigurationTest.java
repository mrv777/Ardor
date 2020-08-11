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

package nxt.http.configuration;

import nxt.BlockchainTest;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.http.callers.GetConfigurationCall;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class GetConfigurationTest extends BlockchainTest {

    @Test
    public void test() {
        JSONObject response = GetConfigurationCall.create().build().invokeNoError();
        for (JO jo : new JA(response.get("properties")).objects()) {
            if ("nxt.isTestnet".equals(jo.getString("name"))) {
                Assert.assertEquals("BOOLEAN", jo.getString("type"));
                Assert.assertEquals("PEER NETWORKING", jo.getString("group"));
                Assert.assertEquals("false", jo.getString("defaultValue"));
                Assert.assertNull(jo.getString("installerValue"));
                return;
            }
        }
        Assert.fail("nxt.isTestnet not found");
    }
}
