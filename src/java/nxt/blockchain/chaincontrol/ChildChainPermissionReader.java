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

import nxt.db.DbIterator;

import java.util.List;

import static java.util.stream.Collectors.toList;

class ChildChainPermissionReader implements PermissionReader {
    private final ChildChainPermissionStore store;

    ChildChainPermissionReader(ChildChainPermissionStore store) {
        this.store = store;
    }

    @Override
    public List<ChildChainPermission> getPermissions(PermissionType permissionType, long granterId, int minHeight, int from, int to) {
        try (DbIterator<ChildChainPermission> dbIterator = store.getPermissions(permissionType, granterId, minHeight, from, to)) {
            return dbIterator.stream().collect(toList());
        }
    }

    @Override
    public ChildChainAccountPermissions getPermissions(long accountId) {
        try (DbIterator<ChildChainPermission> dbIterator = store.get(accountId, -1, -1)) {
            return new ChildChainAccountPermissions(dbIterator.stream().collect(toList()));
        }
    }
}
