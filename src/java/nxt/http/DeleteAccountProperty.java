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
import nxt.account.AccountPropertyDeleteAttachment;
import nxt.blockchain.Attachment;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class DeleteAccountProperty extends CreateTransaction {

    static final DeleteAccountProperty instance = new DeleteAccountProperty();

    private DeleteAccountProperty() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "property", "setter");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account senderAccount = ParameterParser.getSenderAccount(req);
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        if (recipientId == 0) {
            recipientId = senderAccount.getId();
        }
        long setterId = ParameterParser.getAccountId(req, "setter", false);
        if (setterId == 0) {
            setterId = senderAccount.getId();
        }
        String property = Convert.nullToEmpty(req.getParameter("property")).trim();
        if (property.isEmpty()) {
            return JSONResponses.MISSING_PROPERTY;
        }
        Account.AccountProperty accountProperty = Account.getProperty(recipientId, property, setterId);
        if (accountProperty == null) {
            return JSONResponses.UNKNOWN_PROPERTY;
        }
        if (accountProperty.getRecipientId() != senderAccount.getId() && accountProperty.getSetterId() != senderAccount.getId()) {
            return JSONResponses.INCORRECT_PROPERTY;
        }
        Attachment attachment = new AccountPropertyDeleteAttachment(accountProperty.getId());
        return transactionParameters(req, senderAccount, attachment).setRecipientId(recipientId).createTransaction();

    }

    @Override
    public boolean isIgnisOnly() {
        return true;
    }

}
