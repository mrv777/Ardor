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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface PermissionReader {
    List<ChildChainPermission> getPermissions(PermissionType permissionType, long granterId, int minHeight, int from, int to);
    ChildChainAccountPermissions getPermissions(long accountId);

    class ChildChainAccountPermissions {
        private final List<ChildChainPermission> permissions;

        ChildChainAccountPermissions(List<ChildChainPermission> permissions) {
            this.permissions = permissions;
        }

        public List<ChildChainPermission> getPermissions() {
            return permissions;
        }

        public List<ChildChainPermission> getEffectivePermissions() {
            Set<PermissionType> blocked = permissions.stream()
                    .map(ChildChainPermission::getPermissionType)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(PermissionType.class)));
            return permissions.stream()
                    .filter(p -> !blocked.contains(p.getPermissionType().blocker()))
                    .collect(Collectors.toList());
        }
    }
}
