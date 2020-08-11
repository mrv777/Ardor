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

import nxt.addons.JO;
import nxt.blockchain.ChildTransaction;
import nxt.blockchain.FxtChain;
import nxt.blockchain.FxtTransaction;
import nxt.util.Logger;
import org.junit.Before;
import org.junit.Test;

public class CoinExchangeOperationsTest extends AbstractContractTest {
    @Before
    public void deployContracts() {
        ContractTestHelper.deployContract(CoinExchangeOrder.class, null, false);
        ContractTestHelper.deployContract(CoinExchangeCancel.class);
    }

    @Test
    public void buyParentWithChild() {
        Logger.logDebugMessage("Send message to trigger the contract execution");
        sendOrderTrigger(2, 1);

        Logger.logDebugMessage("Contract should submit order transaction now");
        generateBlock();

        Logger.logDebugMessage("Check if coin exchange tx sent on parent chain.");
        FxtTransaction fxtTransaction = testAndGetLastParentTransaction(1, -4, 0,
                a -> a == 0, FxtChain.FXT.ONE_COIN / 2,
                ALICE, null);

        Logger.logDebugMessage("Send trigger for cancel order contract");
        long orderId = fxtTransaction.getId();
        sendCancelTrigger(2, orderId);

        Logger.logDebugMessage("Contract should submit cancel transaction now");
        generateBlock();

        Logger.logDebugMessage("Check if cancel tx sent on parent chain.");
        testAndGetLastParentTransaction(1, -4, 1,
                a -> a == 0, FxtChain.FXT.ONE_COIN / 2,
                ALICE, null);
    }

    @Test
    public void buyChildWithChild() {
        Logger.logDebugMessage("Send message to trigger the contract execution");
        String triggerFullHash = sendOrderTrigger(2, 3);

        Logger.logDebugMessage("Contract should submit order transaction now");
        generateBlock();

        Logger.logDebugMessage("Check if coin exchange tx sent on child chain.");
        ChildTransaction orderTx = testAndGetLastChildTransaction(2, 11, 0,
                a -> a == 0, 4000000,
                ALICE, null, triggerFullHash);

        Logger.logDebugMessage("Send trigger for cancel order contract");
        long orderId = orderTx.getId();
        String cancelFullHash = sendCancelTrigger(2, orderId);

        Logger.logDebugMessage("Contract should submit cancel transaction now");
        generateBlock();

        Logger.logDebugMessage("Check if cancel tx sent on child chain.");
        testAndGetLastChildTransaction(2, 11, 1,
                a -> a == 0, 4000000,
                ALICE, null, cancelFullHash);
    }

    @Test
    public void buyChildWithParent() {
        Logger.logDebugMessage("Send message to trigger the contract execution");
        sendOrderTrigger(1, 2);

        Logger.logDebugMessage("Contract should submit order transaction now");
        generateBlock();

        Logger.logDebugMessage("Check if coin exchange tx sent on parent chain.");
        FxtTransaction orderTx = testAndGetLastParentTransaction(1, -4, 0,
                a -> a == 0, FxtChain.FXT.ONE_COIN / 2,
                ALICE, null);

        Logger.logDebugMessage("Send trigger for cancel order contract");
        long orderId = orderTx.getId();
        sendCancelTrigger(1, orderId);

        Logger.logDebugMessage("Contract should submit cancel transaction now");
        generateBlock();

        Logger.logDebugMessage("Check if cancel tx sent on parent chain.");
        testAndGetLastParentTransaction(1, -4, 1,
                a -> a == 0, FxtChain.FXT.ONE_COIN / 2,
                ALICE, null);
    }

    private String sendOrderTrigger(int from, int to) {
        JO messageJson = new JO();
        messageJson.put("contract", CoinExchangeOrder.class.getSimpleName());
        JO params = new JO();
        params.put("from", from);
        params.put("to", to);
        messageJson.put("params", params);
        String message = messageJson.toJSONString();
        return ContractTestHelper.messageTriggerContract(message);
    }

    private String sendCancelTrigger(int chainId, long orderId) {
        JO messageJson = new JO();
        messageJson.put("contract", CoinExchangeCancel.class.getSimpleName());
        JO params = new JO();
        params.put("chain", chainId);
        params.put("order", orderId);
        messageJson.put("params", params);
        String message = messageJson.toJSONString();
        return ContractTestHelper.messageTriggerContract(message);
    }
}
