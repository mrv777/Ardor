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

package nxt.http;

import nxt.NxtException;
import nxt.account.Account;
import nxt.blockchain.chaincontrol.AddPermissionAttachment;
import nxt.blockchain.chaincontrol.PermissionType;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class AddAccountPermission extends CreateTransaction {
    static final AddAccountPermission instance = new AddAccountPermission();

    private AddAccountPermission() {
        super(new APITag[] {APITag.CHILD_CHAIN_CONTROL, APITag.CREATE_TRANSACTION}, "recipient", "permission");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account sender = ParameterParser.getSenderAccount(req);
        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        PermissionType permissionType = ParameterParser.getPermissionName(req, true);

        return transactionParameters(req, sender, new AddPermissionAttachment(permissionType))
                .setRecipientId(recipientId)
                .createTransaction();
    }
}
