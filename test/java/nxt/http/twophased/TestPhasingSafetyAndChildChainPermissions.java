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

package nxt.http.twophased;

import nxt.BlockchainWithChildChainControlTest;
import nxt.Nxt;
import nxt.blockchain.chaincontrol.PermissionTestUtil;
import nxt.blockchain.chaincontrol.PermissionType;
import nxt.http.APICall;
import nxt.http.twophased.TestCreateTwoPhased.TwoPhasedMoneyTransferBuilder;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static org.junit.Assert.assertEquals;

public class TestPhasingSafetyAndChildChainPermissions extends BlockchainWithChildChainControlTest {
    @Test
    public void testPhasingSafeTransactionSuccess() {
        int duration = 10;

        Object fullHash = new TwoPhasedMoneyTransferBuilder()
                .finishHeight(Nxt.getBlockchain().getHeight() + duration)
                .build()
                .invokeNoError()
                .get("fullHash");

        generateBlock();

        PermissionTestUtil.removePermission(IGNIS, ALICE, PermissionType.CHAIN_USER);

        new APICall.Builder("approveTransaction")
                .param("secretPhrase", CHUCK.getSecretPhrase())
                .param("phasedTransaction", IGNIS.getId() + ":" + fullHash)
                .param("feeNQT", IGNIS.ONE_COIN)
                .build()
                .invokeNoError();

        generateBlocks(duration);

        assertEquals(-50 * IGNIS.ONE_COIN - 2 * IGNIS.ONE_COIN,
                ALICE.getChainBalanceDiff(IGNIS.getId()));
        assertEquals(50 * IGNIS.ONE_COIN, BOB.getChainBalanceDiff(IGNIS.getId()));
        assertEquals(-IGNIS.ONE_COIN, CHUCK.getChainBalanceDiff(IGNIS.getId()));
    }
}