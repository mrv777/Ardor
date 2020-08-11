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

import nxt.NxtException.NotCurrentlyValidException;
import nxt.ae.AssetExchangeTransactionType;
import nxt.blockchain.ChildBlockFxtTransaction;
import nxt.blockchain.ChildBlockFxtTransactionType;
import nxt.blockchain.Transaction;
import nxt.blockchain.TransactionType;
import nxt.shuffling.ShufflingTransactionType;
import nxt.util.Convert;

import java.util.List;

import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;

class ChildChainPermissionChecker implements PermissionChecker {
    private final PermissionReader permissionReader;

    ChildChainPermissionChecker(PermissionReader permissionReader) {
        this.permissionReader = permissionReader;
    }

    @Override
    public void checkTransaction(Transaction transaction) throws NotCurrentlyValidException {
        TransactionType transactionType = transaction.getType();
        if (!requiresPermission(transactionType)) {
            return;
        }
        if (isForbidden(transactionType)) {
            throw new NotCurrentlyValidException("Transactions of type " + transactionType.toString()
                    + " are not allowed on permissioned chains");
        }
        if (transactionType == ChildBlockFxtTransactionType.INSTANCE && ((ChildBlockFxtTransaction) transaction).getChildTransactionsFee() == 0) {
            return;
        }
        checkCanSend(transaction.getSenderId());
        checkCanReceive(transaction.getRecipientId());
    }

    private void checkCanSend(long accountId) throws NotCurrentlyValidException {
        check(accountId);
    }

    private void checkCanReceive(long accountId) throws NotCurrentlyValidException {
        if (accountId == 0) {
            return;
        }
        check(accountId);
    }

    private void check(long accountId) throws NotCurrentlyValidException {
        checkPermission(accountId, CHAIN_USER);
    }

    private boolean requiresPermission(TransactionType transactionType) {
        return !(transactionType instanceof ChildChainControlTransactionType);
    }

    private boolean isForbidden(TransactionType transactionType) {
        return transactionType == ShufflingTransactionType.SHUFFLING_CREATION
                || transactionType == AssetExchangeTransactionType.DIVIDEND_PAYMENT;
    }

    @Override
    public void checkPermission(long accountId, PermissionType permissionType) throws NotCurrentlyValidException {
        if (hasPermission(accountId, permissionType)) {
            return;
        }
        throw new NotCurrentlyValidException(
                String.format("User %s needs permission %s", Convert.rsAccount(accountId), permissionType.name()));
    }

    private boolean hasPermission(long accountId, PermissionType permissionType) {
        PermissionReader.ChildChainAccountPermissions childChainAccountPermissions = permissionReader.getPermissions(accountId);
        List<ChildChainPermission> permissions = childChainAccountPermissions.getPermissions();
        if (permissions.size() == 1) {
            return permissions.get(0).getPermissionType() == permissionType;
        }
        return childChainAccountPermissions.getEffectivePermissions().stream()
                .map(ChildChainPermission::getPermissionType)
                .anyMatch(t -> t == permissionType);
    }
}
