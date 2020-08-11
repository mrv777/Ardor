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

import nxt.Nxt;
import nxt.NxtException;
import nxt.account.Account;
import nxt.blockchain.ChildBlockAttachment;
import nxt.blockchain.ChildTransaction;
import nxt.blockchain.Transaction;
import nxt.blockchain.UnconfirmedTransaction;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transaction created by this call must expire before any transaction bundled.
 * For this reason if deadline not set it is calculated based on transactions bundled. But not greater then configured by 'nxt.defaultChildBlockDeadline' property.
 */
public final class BundleTransactions extends CreateTransaction {
    private static final int defaultChildBlockDeadline = Nxt.getIntProperty("nxt.defaultChildBlockDeadline");

    static final BundleTransactions instance = new BundleTransactions();

    private BundleTransactions() {
        super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "transactionFullHash", "transactionFullHash", "transactionFullHash");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getSenderAccount(req);
        List<ChildTransaction> childTransactions = new ArrayList<>();
        String[] transactionFullHashesValues = req.getParameterValues("transactionFullHash");
        if (transactionFullHashesValues == null || transactionFullHashesValues.length == 0) {
            return JSONResponses.missing("transactionFullHash");
        }
        final int now = Nxt.getEpochTime();
        for (String s : transactionFullHashesValues) {
            byte[] hash = Convert.parseHexString(s);
            UnconfirmedTransaction unconfirmedTransaction = Nxt.getTransactionProcessor().getUnconfirmedTransaction(Convert.fullHashToId(hash));
            if (unconfirmedTransaction == null || !Arrays.equals(hash, unconfirmedTransaction.getFullHash())) {
                return JSONResponses.UNKNOWN_TRANSACTION_FULL_HASH;
            }
            if (!(unconfirmedTransaction.getTransaction() instanceof ChildTransaction)) {
                return JSONResponses.INCORRECT_TRANSACTION;
            }
            if (unconfirmedTransaction.getExpiration() <= now) {
                continue;
            }
            childTransactions.add((ChildTransaction) unconfirmedTransaction.getTransaction());
        }
        ChildBlockAttachment attachment = new ChildBlockAttachment(childTransactions);
        return transactionParameters(req, account, attachment)
                .setDeadline(getDeadline(req, childTransactions))
                .createTransaction();
    }

    /**
     * Calculating minimal deadline for bundling transaction.
     * <ol>
     *  <li>'deadline' request parameter is used if set. </li>
     *  <li>Minimal expiration time of included transactions is used to calculate deadline</li>
     *  <li>But not greater then value configured by nxt property 'nxt.defaultChildBlockDeadline'</li>
     * </ol>
     * Since deadline must result in expiration time before expiration time of any included transaction, we calculate deadline from expiration time and current Epoch Time.
     * Original formula for calculating transaction expiration time is <code>timestamp + deadline * 60</code> see {@link nxt.blockchain.TransactionImpl#getExpiration()}.
     */
    private short getDeadline(HttpServletRequest req, List<ChildTransaction> childTransactions) throws ParameterException {
        int requestedDeadline = ParameterParser.getInt(req, "deadline", 1, Short.MAX_VALUE, false);
        if (requestedDeadline != 0) {
            return (short) requestedDeadline;
        }
        final int timestamp = getTimestamp(req);
        int calculatedDeadline = childTransactions.stream()
                .mapToInt(Transaction::getExpiration)
                .map(expiration -> (expiration - timestamp) / 60)
                .min()
                .orElse(defaultChildBlockDeadline);
        return (short) Math.min(calculatedDeadline, defaultChildBlockDeadline);
    }

    private int getTimestamp(HttpServletRequest req) throws ParameterException {
        int timestamp = ParameterParser.getTimestamp(req);
        if (timestamp != 0) {
            return timestamp;
        }
        return Nxt.getEpochTime() + 1;
    }

}
