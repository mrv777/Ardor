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

public class RemovePermissionAttachment extends Attachment.AbstractAttachment {
    private final PermissionType permissionType;
    private final int height;

    RemovePermissionAttachment(ByteBuffer buffer) {
        super(buffer);
        byte permissionId = buffer.get();
        permissionType = PermissionType.fromId(permissionId);
        height = buffer.getInt();
    }

    RemovePermissionAttachment(JSONObject json) {
        super(json);
        int permissionId =  ((Long)json.get("permission")).intValue();
        permissionType = PermissionType.fromId(permissionId);
        height = ((Long)json.get("height")).intValue();
    }

    public RemovePermissionAttachment(PermissionType permissionType, int height) {
        this.permissionType = permissionType;
        this.height = height;
    }

    PermissionType getPermissionType() {
        return permissionType;
    }

    int getHeight() {
        return height;
    }

    @Override
    protected int getMySize() {
        return 1 + 4;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.put((byte) permissionType.getId());
        buffer.putInt(height);
    }

    @Override
    protected void putMyJSON(JSONObject json) {
        json.put("permission", permissionType.getId());
        json.put("height", height);
    }

    @Override
    public TransactionType getTransactionType() {
        return ChildChainControlTransactionType.REMOVE_PERMISSION;
    }
}
