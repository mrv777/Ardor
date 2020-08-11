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

import nxt.NxtException;
import nxt.blockchain.Transaction;
import nxt.http.APITag;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

class NonePermissionPolicy implements PermissionPolicy {
    static final PermissionPolicy POLICY_INSTANCE = new NonePermissionPolicy();
    private static final PermissionChecker CHECKER_INSTANCE = new PermissionChecker() {
        @Override
        public void checkTransaction(Transaction transaction) throws NxtException.NotCurrentlyValidException {
            if (transaction.getType() instanceof ChildChainControlTransactionType) {
                throw new NxtException.NotCurrentlyValidException("ChildChainControl transactions not allowed on permissionless child chains");
            }
        }

        @Override
        public void checkPermission(long accountId, PermissionType permissionType) {
        }
    };

    private static final PermissionWriter WRITER_INSTANCE = new PermissionWriter() {
        @Override
        void addPermission(long accountId, PermissionType permissionType) {
        }

        @Override
        void removePermission(long accountId, PermissionType permissionType) {
        }

        @Override
        void removePermission(long accountId, PermissionType permissionType, int height) {
        }

        @Override
        public void addInitialPermission(long accountId, PermissionType permissionType) {
        }
    };

    private static final PermissionReader READER_INSTANCE = new PermissionReader() {
        private final ChildChainAccountPermissions emptyAccountPermissions = new ChildChainAccountPermissions(emptyList());

        @Override
        public List<ChildChainPermission> getPermissions(PermissionType permissionType, long granterId, int minHeight, int from, int to) {
            return emptyList();
        }

        @Override
        public ChildChainAccountPermissions getPermissions(long accountId) {
            return emptyAccountPermissions;
        }
    };

    @Override
    public PermissionChecker getPermissionChecker() {
        return CHECKER_INSTANCE;
    }

    @Override
    public PermissionWriter getPermissionWriter(long granterId) {
        return WRITER_INSTANCE;
    }

    @Override
    public PermissionReader getPermissionReader() {
        return READER_INSTANCE;
    }

    @Override
    public Set<APITag> getDisabledAPITags() {
        return Collections.singleton(APITag.CHILD_CHAIN_CONTROL);
    }

    @Override
    public String getName() {
        return PermissionPolicyType.NONE.name();
    }
}
