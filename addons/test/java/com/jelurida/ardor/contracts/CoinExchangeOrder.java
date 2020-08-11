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

package com.jelurida.ardor.contracts;

import nxt.addons.AbstractContract;
import nxt.addons.ContractInvocationParameter;
import nxt.addons.ContractParametersProvider;
import nxt.addons.JO;
import nxt.addons.TransactionContext;
import nxt.addons.ValidateChain;
import nxt.addons.ValidateContractRunnerIsRecipient;
import nxt.blockchain.Chain;
import nxt.http.callers.ExchangeCoinsCall;

/**
 * Test contract that places a coin exchange order.
 *
 * @see CoinExchangeOperationsTest
 */
public class CoinExchangeOrder extends AbstractContract {
    @ContractParametersProvider
    public interface Params {
        @ContractInvocationParameter
        int from();

        @ContractInvocationParameter
        int to();
    }

    @Override
    @ValidateContractRunnerIsRecipient
    @ValidateChain(accept = 2)
    public JO processTransaction(TransactionContext context) {
        Params params = context.getParams(Params.class);
        ExchangeCoinsCall tx = ExchangeCoinsCall.create(params.from())
                .exchange(params.to())
                .quantityQNT(2 * Chain.getChain(params.from()).ONE_COIN)
                .priceNQTPerCoin(Chain.getChain(params.from()).ONE_COIN);
        return context.createTransaction(tx);
    }
}
