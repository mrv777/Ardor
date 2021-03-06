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

package nxt.blockchain;

import nxt.NxtException;

import java.util.Collection;
import java.util.List;

public interface FxtTransaction extends Transaction {

    interface Builder extends Transaction.Builder {

        FxtTransaction build() throws NxtException.NotValidException;

        FxtTransaction build(byte[] privateKey) throws NxtException.NotValidException;

        FxtTransaction build(byte[] privateKey, boolean isVoucher) throws NxtException.NotValidException;

    }

    Collection<? extends ChildTransaction> getChildTransactions();

    List<? extends ChildTransaction> getSortedChildTransactions();

    void setChildTransactions(List<? extends ChildTransaction> childTransactions, byte[] blockHash) throws NxtException.NotValidException;

}
