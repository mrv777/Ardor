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

import nxt.NxtException;
import nxt.NxtException.NotValidException;
import nxt.account.Account;
import nxt.account.AccountLedger.LedgerEvent;
import nxt.blockchain.Attachment.AbstractAttachment;
import nxt.blockchain.ChildTransactionImpl;
import nxt.blockchain.ChildTransactionType;
import nxt.blockchain.Transaction;
import nxt.blockchain.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

public abstract class ChildChainControlTransactionType extends ChildTransactionType {
    private static final byte SUBTYPE_ADD_PERMISSION = 0;
    private static final byte SUBTYPE_REMOVE_PERMISSION = 1;

    public static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_ADD_PERMISSION:
                return ADD_PERMISSION;
            case SUBTYPE_REMOVE_PERMISSION:
                return REMOVE_PERMISSION;
            default:
                return null;
        }
    }

    @Override
    protected boolean applyAttachmentUnconfirmed(ChildTransactionImpl transaction, Account senderAccount) {
        return true;
    }

    @Override
    protected void undoAttachmentUnconfirmed(ChildTransactionImpl transaction, Account senderAccount) {
    }

    @Override
    public byte getType() {
        return ChildTransactionType.TYPE_CHILD_CHAIN_CONTROL;
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        long senderId = transaction.getSenderId();
        long recipientId = transaction.getRecipientId();
        String recipientIdString = String.valueOf(recipientId);
        if  (senderId == recipientId) {
            return isDuplicate(ADD_PERMISSION, recipientIdString, duplicates, true);
        }
        String senderIdString = String.valueOf(senderId);
        return isDuplicate(ADD_PERMISSION, senderIdString, duplicates, false)
                || isDuplicate(ADD_PERMISSION, recipientIdString, duplicates, true);
    }

    static final ChildChainControlTransactionType ADD_PERMISSION = new ChildChainControlTransactionType() {
        @Override
        protected void validateAttachment(ChildTransactionImpl transaction) throws NxtException.ValidationException {
            PermissionType permissionType = ((AddPermissionAttachment) transaction.getAttachment()).getPermissionType();
            PermissionType requiredPermission = ChildChainPermissionWriter.getRequiredPermission(permissionType);
            transaction.getChain().getPermissionChecker().checkPermission(transaction.getSenderId(), requiredPermission);
        }

        @Override
        protected void applyAttachment(ChildTransactionImpl transaction, Account senderAccount, Account recipientAccount) {
            PermissionType permissionType = ((AddPermissionAttachment) transaction.getAttachment()).getPermissionType();
            transaction.getChain().getPermissionWriter(senderAccount.getId())
                    .addPermission(recipientAccount.getId(), permissionType);
        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_ADD_PERMISSION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ADD_PERMISSION;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws NotValidException {
            try {
                return new AddPermissionAttachment(buffer);
            } catch (RuntimeException e) {
                throw new NotValidException("Failed to parse byte buffer to AddPermissionAttachment", e);
            }
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) throws NotValidException {
            try {
                return new AddPermissionAttachment(attachmentData);
            } catch (RuntimeException e) {
                throw new NotValidException("Failed to parse JSON to AddPermissionAttachment", e);
            }
        }

        @Override
        public String getName() {
            return "AddPermission";
        }
    };

    static final ChildChainControlTransactionType REMOVE_PERMISSION = new ChildChainControlTransactionType() {
        @Override
        protected void validateAttachment(ChildTransactionImpl transaction) throws NxtException.ValidationException {
            PermissionType permissionType = ((RemovePermissionAttachment) transaction.getAttachment()).getPermissionType();
            PermissionChecker permissionChecker = transaction.getChain().getPermissionChecker();
            long granterId = transaction.getSenderId();

            permissionChecker.checkPermission(granterId, ChildChainPermissionWriter.getRequiredPermission(permissionType));
        }

        @Override
        protected void applyAttachment(ChildTransactionImpl transaction, Account senderAccount, Account recipientAccount) {
            RemovePermissionAttachment attachment = (RemovePermissionAttachment) transaction.getAttachment();
            PermissionType permissionType = attachment.getPermissionType();
            int height = attachment.getHeight();
            PermissionWriter permissionWriter = transaction.getChain().getPermissionWriter(senderAccount.getId());
            if (height >= 0) {
                permissionWriter.removePermission(recipientAccount.getId(), permissionType, height);
            } else {
                permissionWriter.removePermission(recipientAccount.getId(), permissionType);
            }
        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_REMOVE_PERMISSION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.REMOVE_PERMISSION;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws NotValidException {
            try {
                return new RemovePermissionAttachment(buffer);
            } catch (RuntimeException e) {
                throw new NotValidException("Failed to parse AddPermissionAttachment", e);
            }
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) throws NotValidException {
            try {
                return new RemovePermissionAttachment(attachmentData);
            } catch (RuntimeException e) {
                throw new NotValidException("Failed to parse AddPermissionAttachment", e);
            }
        }

        @Override
        public String getName() {
            return "RemovePermission";
        }
    };
}
