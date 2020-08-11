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

import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException.NotCurrentlyValidException;
import nxt.env.ServerStatus;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static nxt.blockchain.chaincontrol.PermissionType.BLOCKED_CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_ADMIN;
import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;
import static nxt.blockchain.chaincontrol.PermissionType.MASTER_ADMIN;

class ChildChainPermissionWriter extends PermissionWriter {
    private final ChildChainPermissionStore store;
    private final long granterId;

    ChildChainPermissionWriter(ChildChainPermissionStore store, long granterId) {
        this.store = store;
        this.granterId = granterId;
    }

    @Override
    void addPermission(long accountId, PermissionType permissionType) {
        store.save(accountId, permissionType, granterId);
    }

    @Override
    void removePermission(long accountId, PermissionType permissionType) {
        store.remove(accountId, permissionType);
    }

    @Override
    void removePermission(long accountId, PermissionType permissionType, int height) {
        removePermission(accountId, permissionType);
        for (PermissionType permission : getCascadePermission(permissionType)) {
            store.removeByGranter(permission, accountId, height);
        }
    }

    @Override
    public void addInitialPermission(long accountId, PermissionType permissionType) {
        if (!Constants.isAutomatedTest && Nxt.getServerStatus() != ServerStatus.BEFORE_DATABASE) {
            throw new IllegalStateException("Setting initial permissions only allowed during tests or in DbVersion");
        }
        store.saveInitial(accountId, permissionType, granterId);
    }

    static List<PermissionType> getCascadePermission(PermissionType permissionType) {
        switch (permissionType) {
            case MASTER_ADMIN:
                throw new IllegalArgumentException("Can't change permission: " + permissionType);
            case CHAIN_ADMIN:
                return Arrays.asList(CHAIN_USER, BLOCKED_CHAIN_USER);
            case BLOCKED_CHAIN_USER:
            case BLOCKED_CHAIN_ADMIN:
            case CHAIN_USER:
                return emptyList();
        }
        throw new IllegalArgumentException("Not supported permission: " + permissionType);
    }

    static PermissionType getRequiredPermission(PermissionType affectedPermission) throws NotCurrentlyValidException {
        switch (affectedPermission) {
            case MASTER_ADMIN:
                throw new NotCurrentlyValidException("Can't change permission: " + affectedPermission);
            case CHAIN_ADMIN:
            case BLOCKED_CHAIN_ADMIN:
                return MASTER_ADMIN;
            case CHAIN_USER:
            case BLOCKED_CHAIN_USER:
                return CHAIN_ADMIN;
        }
        throw new IllegalArgumentException("Not supported permission: " + affectedPermission);
    }

    // visible for test code
    ChildChainPermissionStore getStore() {
        return store;
    }
}
