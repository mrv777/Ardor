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
import nxt.http.callers.AddAccountPermissionCall;
import nxt.http.callers.RemoveAccountPermissionCall;
import nxt.http.callers.SendMoneyCall;
import org.junit.Before;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.grantPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.removePermission;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;
import static org.junit.Assert.assertEquals;

public class PerChainControlETETest extends BlockchainWithChildChainControlTest {
    private static final TesterWrapper masterAdmin = new TesterWrapper(CHUCK);
    private static final TesterWrapper chainAdmin = new TesterWrapper(DAVE);
    private static final TesterWrapper chainUser = new TesterWrapper(ALICE);
    private static final ChildChain childChain = ChildChain.AEUR;

    @Before
    public void setUp() {
        generateBlock();

        removePermission(childChain, masterAdmin.getTester(), CHAIN_USER);
        removePermission(childChain, chainUser.getTester(), CHAIN_USER);
        removePermission(childChain, chainAdmin.getTester(), CHAIN_USER);

        grantPermission(childChain, masterAdmin.getTester(), MASTER_ADMIN);
        grantPermission(childChain, masterAdmin.getTester(), CHAIN_ADMIN);
    }

    @Test
    public void testAcceptTransactionOnControlledChain() {
        masterAdmin.giveChainUserPermission(chainUser);
        chainUser.sendMoneyTo(BOB);
        assertSendMoneySuccess(BOB);
    }

    @Test
    public void testDelegateKYCOnChain() {
        masterAdmin.giveChainAdminPermission(chainAdmin);
        chainAdmin.giveChainUserPermission(chainUser);
        chainUser.sendMoneyTo(BOB);
        assertSendMoneySuccess(BOB);
    }

    @Test
    public void testRejectedDelegateKYCOnChain() {
        //        delegateKYCTo(DAVE); // not delegated
        InvocationError actual = chainAdmin.addPermissionFailed(chainUser.wrappedTester, CHAIN_USER);
        assertEquals("User " + chainAdmin.wrappedTester.getRsAccount() + " needs permission " + CHAIN_ADMIN.name(),
                actual.getErrorDescription());
    }

    @Test
    public void testRejectTransactionOnControlledChain() {
        masterAdmin.removeChainUserPermission(ALICE);
        chainUser.sendMoneyTo(BOB);
        assertSendMoneyIgnored(BOB);
    }

    @Test
    public void testRejectTransactionFromBlockedUser() {
        masterAdmin.blockChainUser(ALICE);
        masterAdmin.giveChainUserPermission(ALICE);
        chainUser.sendMoneyTo(BOB);
        assertSendMoneyIgnored(BOB);
    }

    private void assertSendMoneySuccess(Tester recipient) {
        assertEquals(100 * childChain.ONE_COIN, recipient.getChainBalanceDiff(childChain.getId()));
    }

    private void assertSendMoneyIgnored(Tester recipient) {
        assertEquals(0, recipient.getChainBalanceDiff(childChain.getId()));
    }

    private static class TesterWrapper {
        private final Tester wrappedTester;

        TesterWrapper(Tester sender) {
            this.wrappedTester = sender;
        }

        void giveChainAdminPermission(TesterWrapper chainAdmin) {
            giveChainAdminPermission(chainAdmin.wrappedTester);
        }

        void giveChainAdminPermission(Tester chainAdmin) {
            addPermission(chainAdmin, CHAIN_ADMIN);
        }

        private void addPermission(Tester chainAdmin, PermissionType permissionType) {
            AddAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(wrappedTester.getSecretPhrase())

                    .recipient(chainAdmin.getStrId())
                    .permission(permissionType.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeNoError();

            generateBlock();
        }

        private InvocationError addPermissionFailed(Tester recipient, PermissionType permissionType) {
            return AddAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(wrappedTester.getSecretPhrase())

                    .recipient(recipient.getStrId())
                    .permission(permissionType.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeWithError();
        }

        private void removePermission(Tester endUser, PermissionType permission) {
            RemoveAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(wrappedTester.getSecretPhrase())
                    .recipient(endUser.getId())
                    .permission(permission.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeNoError();

            generateBlock();
        }

        void removeChainUserPermission(Tester endUser) {
            removePermission(endUser, CHAIN_USER);
        }


        void blockChainUser(Tester endUser) {
            addPermission(endUser, BLOCKED_CHAIN_USER);
        }

        void blockChainAdmin(Tester endUser) {
            addPermission(endUser, BLOCKED_CHAIN_ADMIN);
        }

        void giveChainUserPermission(Tester endUser) {
            addPermission(endUser, CHAIN_USER);
        }

        void sendMoneyTo(Tester recipient) {
            SendMoneyCall.create(childChain.getId())
                    .secretPhrase(wrappedTester.getSecretPhrase())
                    .recipient(recipient.getId())
                    .param("amountNQT", 100 * childChain.ONE_COIN)
                    .param("feeNQT", IGNIS.ONE_COIN)
                    .build()
                    .invoke();

            generateBlock();
        }

        void giveChainUserPermission(TesterWrapper endUser) {
            giveChainUserPermission(endUser.wrappedTester);
        }

        long getUserId() {
            return wrappedTester.getId();
        }

        public Tester getTester() {
            return wrappedTester;
        }
    }
}
