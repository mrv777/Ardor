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

package nxt.http.accountControl;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.RequireNonePermissionPolicyTestsCategory;
import nxt.Tester;
import nxt.blockchain.ChildChain;
import nxt.blockchain.chaincontrol.PermissionPolicyType;
import nxt.http.APICall;
import nxt.http.callers.SendMoneyCall;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RequireNonePermissionPolicyTestsCategory.class)
public class PermissionPolicyChangeTest extends BlockchainTest {

    @Test
    public void testPolicyChange() {
        ChildChain childChain = ChildChain.AEUR;
        Tester UNPERMISSIONED = new Tester("unpermissioned tester passphrase");
        Assert.assertEquals(childChain.getPermissionPolicy().getName(), PermissionPolicyType.NONE.name());
        JSONObject response = SendMoneyCall.create(childChain.getId())
                .secretPhrase(RIKER.getSecretPhrase())
                .recipient(UNPERMISSIONED.getId())
                .param("amountNQT", childChain.ONE_COIN)
                .param("feeNQT", childChain.ONE_COIN)
                .build()
                .invokeNoError();
        Logger.logDebugMessage(response.toString());
        for (int i = 0; i < Constants.PERMISSIONED_AEUR_BLOCK; i++) {
            generateBlock();
        }
        Assert.assertEquals(childChain.getPermissionPolicy().getName(), PermissionPolicyType.CHILD_CHAIN.name());
        APICall.InvocationError error = SendMoneyCall.create(childChain.getId())
                .secretPhrase(RIKER.getSecretPhrase())
                .recipient(UNPERMISSIONED.getId())
                .param("amountNQT", childChain.ONE_COIN)
                .param("feeNQT", childChain.ONE_COIN)
                .build()
                .invokeWithError();
        Assert.assertEquals("User " + UNPERMISSIONED.getRsAccount() + " needs permission CHAIN_USER", error.getErrorDescription());
        Logger.logDebugMessage(error.getErrorDescription());
    }
}
