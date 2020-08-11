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

package nxt.blockchain;

import nxt.BlockchainTest;
import nxt.http.APITag;
import org.junit.Assume;
import org.junit.Test;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.isChildChainPolicy;
import static org.junit.Assert.assertEquals;

public class ChildChainTest extends BlockchainTest {
    @Test
    public void testGetDisabledAPITagsPolicyChildChain() {
        ChildChain chain = ChildChain.IGNIS;
        Assume.assumeTrue(isChildChainPolicy(chain));
        assertEquals(emptySet(), chain.getDisabledAPITags());
    }

    @Test
    public void testGetDisabledAPITagsPolicyNone() {
        ChildChain chain = ChildChain.IGNIS;
        Assume.assumeFalse(isChildChainPolicy(chain));
        assertEquals(singleton(APITag.CHILD_CHAIN_CONTROL), chain.getDisabledAPITags());
    }
}