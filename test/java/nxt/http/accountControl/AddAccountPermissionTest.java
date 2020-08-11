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

package nxt.http.accountControl;

import nxt.BlockchainWithChildChainControlTest;
import nxt.blockchain.ChildChain;
import nxt.http.APICall.InvocationError;
import nxt.http.callers.AddAccountPermissionCall;
import org.junit.Before;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.assertHasPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.assertNoPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.grantPermission;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;
import static org.junit.Assert.assertEquals;

public class AddAccountPermissionTest extends BlockchainWithChildChainControlTest {
    private final ChildChain childChain = IGNIS;

    @Before
    public void setUp() {
        generateBlock();

        grantPermission(childChain, CHUCK, MASTER_ADMIN);
    }

    @Test
    public void addAccountPermission() {
        AddAccountPermissionCall.create(childChain.getId())

                .secretPhrase(CHUCK.getSecretPhrase())

                .recipient(ALICE.getId())
                .permission(CHAIN_ADMIN.name())
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeNoError();
        generateBlock();

        assertHasPermission(childChain, ALICE, CHAIN_ADMIN);
    }

    @Test
    public void addAccountPermissionFailed() {
        InvocationError actual = AddAccountPermissionCall.create(childChain.getId())

                .secretPhrase(ALICE.getSecretPhrase())

                .recipient(ALICE.getId())
                .permission(CHAIN_ADMIN.name())
                .feeNQT(IGNIS.ONE_COIN)
                .build().invokeWithError();

        assertEquals("User " + ALICE.getRsAccount() + " needs permission " + MASTER_ADMIN.name(),
                actual.getErrorDescription());

        generateBlock();

        assertNoPermission(childChain, ALICE, CHAIN_ADMIN);
    }
}