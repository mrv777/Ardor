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

import nxt.blockchain.Attachment;
import nxt.blockchain.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class AddPermissionAttachment extends Attachment.AbstractAttachment {
    private final PermissionType permissionType;

    AddPermissionAttachment(ByteBuffer buffer) {
        super(buffer);
        byte permissionId = buffer.get();
        permissionType = PermissionType.fromId(permissionId);
    }

    AddPermissionAttachment(JSONObject json) {
        super(json);
        int permissionId =  ((Long)json.get("permission")).intValue();
        permissionType = PermissionType.fromId(permissionId);
    }

    public AddPermissionAttachment(PermissionType permissionType) {
        this.permissionType = permissionType;
    }

    PermissionType getPermissionType() {
        return permissionType;
    }

    @Override
    protected int getMySize() {
        return 1;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.put((byte) permissionType.getId());
    }

    @Override
    protected void putMyJSON(JSONObject json) {
        json.put("permission", permissionType.getId());
    }

    @Override
    public TransactionType getTransactionType() {
        return ChildChainControlTransactionType.ADD_PERMISSION;
    }
}
