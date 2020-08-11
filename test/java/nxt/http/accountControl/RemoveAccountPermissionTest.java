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
import nxt.Tester;
import nxt.blockchain.ChildChain;
import nxt.blockchain.chaincontrol.PermissionType;
import nxt.http.APICall.InvocationError;
import nxt.http.callers.RemoveAccountPermissionCall;
import org.junit.Before;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.assertHasPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.assertNoPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.grantPermission;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;
import static org.junit.Assert.assertEquals;

public class RemoveAccountPermissionTest extends BlockchainWithChildChainControlTest {
    private final ChildChain childChain = IGNIS;
    private final Tester someChainUser = new Tester("some chain user password");
    private final Tester someMasterAdmin = new Tester("some master admin password");

    @Before
    public void setUp() {
        generateBlock();

        grantPermission(childChain, CHUCK, MASTER_ADMIN);
        grantPermission(childChain, CHUCK, CHAIN_ADMIN);
        grantPermission(childChain, ALICE, CHAIN_ADMIN);
        grantPermission(childChain, someChainUser, CHAIN_USER, ALICE);
        grantPermission(childChain, someChainUser, BLOCKED_CHAIN_USER, ALICE);
    }

    @Test
    public void removeAccountPermissionChainAdminCascade() {
        new SimplifiedCall(CHUCK).removePermission(ALICE, CHAIN_ADMIN, 0);

        assertNoPermission(childChain, ALICE, CHAIN_ADMIN);
        assertNoPermission(childChain, someChainUser, CHAIN_USER);
        assertNoPermission(childChain, someChainUser, BLOCKED_CHAIN_USER);
    }

    @Test
    public void removeAccountPermissionMasterAdminRejected() {
        grantPermission(childChain, someMasterAdmin, MASTER_ADMIN);

        InvocationError actual = new SimplifiedCall(CHUCK).removePermissionWithErro(someMasterAdmin, MASTER_ADMIN, 0);

        assertEquals("Can't change permission: " + MASTER_ADMIN.name(), actual.getErrorDescription());
    }

    @Test
    public void removeAccountPermission() {
        new SimplifiedCall(CHUCK).removePermission(ALICE, CHAIN_ADMIN);

        assertNoPermission(childChain, ALICE, CHAIN_ADMIN);
    }

    @Test
    public void removeAccountBlockedPermission() {
        new SimplifiedCall(CHUCK).removePermission(someChainUser, BLOCKED_CHAIN_USER);

        assertNoPermission(childChain, someChainUser, BLOCKED_CHAIN_USER);
        assertHasPermission(childChain, someChainUser, CHAIN_USER);
    }

    @Test
    public void removeAccountBlockedPermission1() {
        new SimplifiedCall(CHUCK).removePermission(someChainUser, BLOCKED_CHAIN_USER, 0);

        assertNoPermission(childChain, someChainUser, BLOCKED_CHAIN_USER);
        assertHasPermission(childChain, someChainUser, CHAIN_USER);
    }

    @Test
    public void removeAccountPermissionFailed() {
        InvocationError actual = new SimplifiedCall(BOB).removePermissionWithError(ALICE, CHAIN_ADMIN);
        assertEquals("User " + BOB.getRsAccount() + " needs permission " + MASTER_ADMIN.name(), actual.getErrorDescription());
    }

    private class SimplifiedCall {
        private final Tester caller;

        private SimplifiedCall(Tester caller) {
            this.caller = caller;
        }

        void removePermission(Tester recipient, PermissionType permission) {
            RemoveAccountPermissionCall.create(childChain.getId())

                    .secretPhrase(caller.getSecretPhrase())

                    .recipient(recipient.getId())
                    .permission(permission.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeNoError();
            generateBlock();
        }

        InvocationError removePermissionWithError(Tester recipient, PermissionType permission) {
            return RemoveAccountPermissionCall.create(childChain.getId())

                    .secretPhrase(caller.getSecretPhrase())

                    .recipient(recipient.getId())
                    .permission(permission.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeWithError();
        }

        void removePermission(Tester recipient, PermissionType permission, int height) {
            RemoveAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(caller.getSecretPhrase())
                    .recipient(recipient.getId())
                    .permission(permission.name())
                    .height(height)
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeNoError();
            generateBlock();
        }

        InvocationError removePermissionWithErro(Tester recipient, PermissionType permission, int height) {
            return RemoveAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(caller.getSecretPhrase())
                    .recipient(recipient.getId())
                    .permission(permission.name())
                    .height(height)
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeWithError();
        }
    }
}