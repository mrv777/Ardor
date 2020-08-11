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

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

public enum PermissionType {
    MASTER_ADMIN, CHAIN_ADMIN, CHAIN_USER, BLOCKED_CHAIN_USER, BLOCKED_CHAIN_ADMIN;
    private static final List<PermissionType> MASTER_ADMIN_CAN_GRANT = unmodifiableList(asList(CHAIN_ADMIN, BLOCKED_CHAIN_ADMIN));
    private static final List<PermissionType> CHAIN_ADMIN_CAN_GRANT = unmodifiableList(asList(CHAIN_USER, BLOCKED_CHAIN_USER));

    int getId() {
        return ordinal();
    }

    static PermissionType fromId(int id) {
        return values()[id];
    }

    public Collection<PermissionType> canGrant() {
        switch (this) {
            case CHAIN_ADMIN:
                return CHAIN_ADMIN_CAN_GRANT;
            case MASTER_ADMIN:
                return MASTER_ADMIN_CAN_GRANT;
            default:
                return emptyList();
        }
    }

    public PermissionType blocker() {
        switch (this) {
            case CHAIN_ADMIN:
                return BLOCKED_CHAIN_ADMIN;
            case CHAIN_USER:
                return BLOCKED_CHAIN_USER;
            default:
                return null;
        }
    }

    public boolean isBlocker() {
        return this == BLOCKED_CHAIN_ADMIN || this == BLOCKED_CHAIN_USER;
    }
}
