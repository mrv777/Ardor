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

import nxt.blockchain.ChildChain;
import nxt.http.APITag;

import java.util.Collections;
import java.util.Set;

class ChildChainPermissionPolicy implements PermissionPolicy {
    private final String name;
    private final ChildChainPermissionStore store;
    private final PermissionChecker permissionChecker;
    private final PermissionReader permissionReader;

    ChildChainPermissionPolicy(String name, ChildChain chain) {
        this.name = name;
        this.store = new ChildChainPermissionStore(chain);
        this.permissionReader = new ChildChainPermissionReader(store);
        this.permissionChecker = new ChildChainPermissionChecker(permissionReader);
    }

    @Override
    public PermissionChecker getPermissionChecker() {
        return permissionChecker;
    }

    @Override
    public PermissionWriter getPermissionWriter(long granterId) {
        return new ChildChainPermissionWriter(store, granterId);
    }

    @Override
    public PermissionReader getPermissionReader() {
        return permissionReader;
    }

    @Override
    public Set<APITag> getDisabledAPITags() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return name;
    }
}
