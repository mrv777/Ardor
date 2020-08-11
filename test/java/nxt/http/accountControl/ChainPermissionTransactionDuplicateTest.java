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
import nxt.http.callers.AddAccountPermissionCall;
import nxt.http.callers.RemoveAccountPermissionCall;
import org.junit.Before;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.assertHasPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.grantPermission;
import static nxt.blockchain.chaincontrol.PermissionTestUtil.removePermission;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;

public class ChainPermissionTransactionDuplicateTest extends BlockchainWithChildChainControlTest {
    private static final TesterWrapper masterAdmin = new TesterWrapper(CHUCK);
    private static final TesterWrapper chainAdmin = new TesterWrapper(DAVE);
    private static final TesterWrapper chainUser = new TesterWrapper(ALICE);
    private static final TesterWrapper chainUser2 = new TesterWrapper(BOB);
    private static final ChildChain childChain = ChildChain.AEUR;

    @Before
    public void setUp() {
        generateBlock();

        removePermission(childChain, masterAdmin.getTester(), CHAIN_USER);
        removePermission(childChain, chainAdmin.getTester(), CHAIN_USER);
        removePermission(childChain, chainUser.getTester(), CHAIN_USER);
        removePermission(childChain, chainUser2.getTester(), CHAIN_USER);

        grantPermission(childChain, masterAdmin.getTester(), MASTER_ADMIN);
        grantPermission(childChain, masterAdmin.getTester(), CHAIN_ADMIN);
        grantPermission(childChain, chainAdmin.getTester(), CHAIN_ADMIN);
        generateBlock();
    }

    @Test
    public void testDifferentGrantersDifferentReceivers() {
        masterAdmin.giveChainUserPermission(chainUser);
        chainAdmin.giveChainUserPermission(chainUser2);
        generateBlock();

        assertHasPermission(childChain, chainUser.wrappedTester, CHAIN_USER);
        assertHasPermission(childChain, chainUser2.wrappedTester, CHAIN_USER);
    }

    @Test
    public void testSameGrantersDifferentReceivers() {
        chainAdmin.giveChainUserPermission(chainUser);
        chainAdmin.giveChainUserPermission(chainUser2);
        generateBlock();

        assertHasPermission(childChain, chainUser.wrappedTester, CHAIN_USER);
        assertHasPermission(childChain, chainUser2.wrappedTester, CHAIN_USER);
    }

    @Test
    public void testDifferentGrantersSameReceiver() {
        chainAdmin.giveChainUserPermission(chainUser);
        masterAdmin.giveChainAdminPermission(chainUser);
        generateBlock();

        assertHasPermission(childChain, chainUser.wrappedTester, CHAIN_USER);
    }

    @Test
    public void testSameReceiverAndGranter() {
        masterAdmin.giveChainUserPermission(chainUser);
        generateBlock();

        masterAdmin.giveChainUserPermission(chainAdmin);
        chainAdmin.removeChainUserPermission(chainUser);
        generateBlock();

        assertHasPermission(childChain, chainUser.wrappedTester, CHAIN_USER);
        assertHasPermission(childChain, chainAdmin.wrappedTester, CHAIN_USER);
    }

    @Test
    public void testGrantToHimself() {
        chainAdmin.giveChainUserPermission(chainAdmin);

        generateBlock();

        assertHasPermission(childChain, chainAdmin.wrappedTester, CHAIN_USER);
    }

    private static class TesterWrapper {
        private final Tester wrappedTester;

        TesterWrapper(Tester sender) {
            this.wrappedTester = sender;
        }

        void giveChainAdminPermission(TesterWrapper chainAdmin) {
            giveChainAdminPermission(chainAdmin.wrappedTester);
        }

        void giveChainAdminPermission(Tester thainAdmin) {
            addPermission(thainAdmin, CHAIN_ADMIN);
        }

        private void addPermission(Tester thainAdmin, PermissionType permissionType) {
            AddAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(wrappedTester.getSecretPhrase())

                    .recipient(thainAdmin.getStrId())
                    .permission(permissionType.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeNoError();
        }

        private void removePermission(Tester endUser, PermissionType permission) {
            RemoveAccountPermissionCall.create(childChain.getId())
                    .secretPhrase(wrappedTester.getSecretPhrase())
                    .recipient(endUser.getId())
                    .permission(permission.name())
                    .feeNQT(IGNIS.ONE_COIN)
                    .build().invokeNoError();
        }

        void removeChainUserPermission(TesterWrapper endUser) {
            removeChainUserPermission(endUser.wrappedTester);
        }

        void removeChainUserPermission(Tester endUser) {
            removePermission(endUser, CHAIN_USER);
        }


        void giveChainUserPermission(Tester endUser) {
            addPermission(endUser, CHAIN_USER);
        }

        void giveChainUserPermission(TesterWrapper endUser) {
            giveChainUserPermission(endUser.wrappedTester);
        }

        public Tester getTester() {
            return wrappedTester;
        }
    }
}
