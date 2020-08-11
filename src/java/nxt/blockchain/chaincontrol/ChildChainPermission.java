/*
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

import nxt.Nxt;

public class ChildChainPermission {
    private final PermissionType permissionType;
    private final long accountId;
    private long granterId;
    private int height;

    ChildChainPermission(long accountId, PermissionType permissionType, long granterId, int height) {
        this.permissionType = permissionType;
        this.accountId = accountId;
        this.granterId = granterId;
        this.height = height;
    }

    public PermissionType getPermissionType() {
        return permissionType;
    }

    public long getGranterId() {
        return granterId;
    }

    public long getAccountId() {
        return accountId;
    }

    public int getHeight() {
        return height;
    }

    void setGranterId(long granterId) {
        this.granterId = granterId;
        this.height = Nxt.getBlockchain().getHeight();
    }

    @Override
    public String toString() {
        return "ChildChainPermission{" +
                "type=" + permissionType +
                ", accountId=" + accountId +
                ", granterId=" + granterId +
                ", height=" + height +
                '}';
    }
}
