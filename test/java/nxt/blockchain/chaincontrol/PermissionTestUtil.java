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

package nxt.blockchain.chaincontrol;

import nxt.Tester;
import nxt.blockchain.ChildChain;
import nxt.dbschema.Db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PermissionTestUtil {
    public static boolean isChildChainPolicy(ChildChain childChain) {
        return PermissionPolicyType.CHILD_CHAIN.name().equals(childChain.getPermissionPolicy().getName());
    }

    public static void grantPermission(ChildChain chain, Tester account, PermissionType permission) {
        grantPermission(chain, account.getId(), permission);
    }

    public static void grantPermission(ChildChain chain, Tester account, PermissionType permission, Tester granter) {
        grantPermission(chain, account.getId(), permission, granter.getId());
    }

    public static void grantPermission(ChildChain chain, long accountId, PermissionType permission) {
        grantPermission(chain, accountId, permission, 0);
    }

    public static void grantPermission(ChildChain chain, long accountId, PermissionType permission, long granterId) {
        getPermissionStore(chain).save(accountId, permission, granterId);
    }

    public static void removeAllPermissions(ChildChain chain) {
        getPermissionStore(chain).removeAll();
    }

    public static void removePermission(ChildChain chain, Tester account, PermissionType permission) {
        getPermissionStore(chain).remove(account.getId(), permission);
    }

    public static void assertNoPermission(ChildChain chain, Tester account, PermissionType permission) {
        assertNull(getPermissionStore(chain).get(account.getId(), permission));
    }

    public static void assertHasPermission(ChildChain chain, Tester account, PermissionType permission) {
        assertNotNull(getPermissionStore(chain).get(account.getId(), permission));
    }

    private static Store getPermissionStore(ChildChain chain) {
        PermissionWriter permissionWriter = chain.getPermissionWriter(0);
        if (permissionWriter instanceof ChildChainPermissionWriter) {
            return new RealStore(getChildChainPermissionStore(permissionWriter));
        } else {
            return new NoOpStore();
        }
    }

    static ChildChainPermissionStore getChildChainPermissionStore(ChildChain chain) {
        return getChildChainPermissionStore(chain.getPermissionWriter(0));
    }

    private static ChildChainPermissionStore getChildChainPermissionStore(PermissionWriter permissionWriter) {
        if (permissionWriter instanceof ChildChainPermissionWriter) {
            return ((ChildChainPermissionWriter) permissionWriter).getStore();
        } else {
            fail("must not be called for tests without real Permission Policy");
            return null;
        }
    }

    private interface Store {
        ChildChainPermission get(long accountId, PermissionType name);

        void remove(long accountId, PermissionType name);

        void removeAll();

        ChildChainPermission save(long accountId, PermissionType name, long granterId);
    }

    private static class RealStore implements Store {
        private final ChildChainPermissionStore store;

        private RealStore(ChildChainPermissionStore store) {
            this.store = store;
        }

        @Override
        public ChildChainPermission get(long accountId, PermissionType name) {
            return store.get(accountId, name);
        }

        @Override
        public void remove(long accountId, PermissionType name) {
            Db.db.runInDbTransaction(() -> store.remove(accountId, name));
        }

        @Override
        public void removeAll() {
            Db.db.runInDbTransaction(store::removeAll);
        }

        @Override
        public ChildChainPermission save(long accountId, PermissionType name, long granterId) {
            return Db.db.callInDbTransaction(() -> store.save(accountId, name, granterId));
        }
    }

    private static class NoOpStore implements Store {
        @Override
        public ChildChainPermission get(long accountId, PermissionType name) {
            fail("must not be called for tests without real Permission Policy");
            return null;
        }

        @Override
        public void remove(long accountId, PermissionType name) {
        }

        @Override
        public void removeAll() {
        }

        @Override
        public ChildChainPermission save(long accountId, PermissionType name, long granterId) {
            return null;
        }
    }
}
