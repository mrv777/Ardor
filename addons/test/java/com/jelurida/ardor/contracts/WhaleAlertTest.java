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

import nxt.Nxt;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.http.callers.GetAssetCall;
import nxt.http.callers.IssueAssetCall;
import nxt.http.callers.PlaceAskOrderCall;
import nxt.http.callers.PlaceBidOrderCall;
import nxt.http.callers.SendMoneyCall;
import nxt.http.callers.SetAccountInfoCall;
import nxt.http.callers.TransferAssetCall;
import nxt.http.callers.TriggerContractByRequestCall;
import nxt.http.responses.AssetEntityResponse;
import nxt.http.responses.ErrorResponse;
import nxt.http.responses.TransactionResponse;
import nxt.util.Convert;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.jelurida.ardor.contracts.OrderBean.order;
import static java.util.Arrays.asList;

public class WhaleAlertTest extends AbstractContractTest {

    @Test
    public void sendMoneyAlert() {
        JO setInfoResponse = SetAccountInfoCall.create(2).name("Alice").secretPhrase(ALICE.getSecretPhrase()).feeNQT(ChildChain.IGNIS.ONE_COIN).call();
        Logger.logInfoMessage(setInfoResponse.toJSONString());
        ContractTestHelper.deployContract(SlackNotifier.class);
        String contractName = ContractTestHelper.deployContract(WhaleAlert.class);

        // Trigger contract without transactions
        JO response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).call();
        Assert.assertEquals(1002, response.getInt("errorCode"));

        // Submit high value transaction and make sure it raises an alert
        response = SendMoneyCall.create(2).amountNQT(1000L * ChildChain.IGNIS.ONE_COIN).recipient(BOB.getRsAccount()).privateKey(ALICE.getPrivateKey()).feeNQT(ChildChain.IGNIS.ONE_COIN).call();
        Logger.logInfoMessage(response.toJSONString());
        generateBlock();

        response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).call();
        JA messages = response.getArray("messages");
        Assert.assertEquals(String.format(WhaleAlert.ALERT_MESSAGE_FORMAT, "Testnet", 1000.00000000, ChildChain.IGNIS.getName(), "coin", "paid", Nxt.getBlockchain().getHeight(), " from ARDOR-XK4R-7VJU-6EQG-7R335 (Alice) to ARDOR-EVHD-5FLM-3NMQ-G46NR"), messages.get(0).getString("message"));

        // Submit low value transaction and make sure it does not raise an alert
        response = SendMoneyCall.create(2).amountNQT(ChildChain.IGNIS.ONE_COIN).recipient(BOB.getRsAccount()).privateKey(ALICE.getPrivateKey()).feeNQT(ChildChain.IGNIS.ONE_COIN).call();
        Logger.logInfoMessage(response.toJSONString());
        generateBlock();

        response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).call();
        ErrorResponse errorResponse = ErrorResponse.create(response);
        Assert.assertTrue(errorResponse.isError());
        Assert.assertEquals(1002, errorResponse.getErrorCode());
    }
    @Test
    public void coinExchangeAlert() {
        ContractTestHelper.deployContract(SlackNotifier.class);
        String contractName = ContractTestHelper.deployContract(WhaleAlert.class);

        List<OrderBean> orders = asList(order(100, 1000, ChildChain.IGNIS, FxtChain.FXT), order(100, 1000, FxtChain.FXT, ChildChain.IGNIS));
        OrderBean.createOrderBook(orders);
        generateBlock();

        JO response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).call();
        JA messages = response.getArray("messages");
        Assert.assertEquals(String.format(WhaleAlert.ALERT_MESSAGE_FORMAT, "Testnet", 1000.00000000, FxtChain.FXT_NAME, "coin", "exchanged", Nxt.getBlockchain().getHeight(), ""), messages.get(0).getString("message"));
        Assert.assertEquals(String.format(WhaleAlert.ALERT_MESSAGE_FORMAT, "Testnet", 1000.00000000, ChildChain.IGNIS.getName(), "coin", "exchanged", Nxt.getBlockchain().getHeight(), ""), messages.get(1).getString("message"));

        generateBlock();
        response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).call();
        Assert.assertEquals(1002, response.getInt("errorCode"));
    }

    @Test
    public void assetTransferAlert() {
        JO setInfoResponse = SetAccountInfoCall.create(2).name("Alice").secretPhrase(ALICE.getSecretPhrase()).feeNQT(ChildChain.IGNIS.ONE_COIN).call();
        Logger.logInfoMessage(setInfoResponse.toJSONString());

        TransactionResponse issueAssetTransaction = IssueAssetCall.create(2).name("myAsset").description("...").quantityQNT(123456789).decimals(4).privateKey(ALICE.getPrivateKey()).feeNQT(100 * ChildChain.IGNIS.ONE_COIN).getCreatedTransaction();
        generateBlock();

        // TODO create a getAsset wrapper that returns an asset response
        AssetEntityResponse myAsset = AssetEntityResponse.create(GetAssetCall.create().asset(Convert.fullHashToId(issueAssetTransaction.getFullHash())).call());
        ContractTestHelper.deployContract(SlackNotifier.class);
        String contractName = ContractTestHelper.deployContract(WhaleAlert.class);

        // Submit high value transaction and make sure it raises an alert
        JO assetTransferResponse = TransferAssetCall.create(2).asset(myAsset.getAsset()).recipient(BOB.getRsAccount()).quantityQNT(100000L).recipient(BOB.getRsAccount()).privateKey(ALICE.getPrivateKey()).feeNQT(ChildChain.IGNIS.ONE_COIN).call();
        Logger.logInfoMessage(assetTransferResponse.toJSONString());
        Assert.assertFalse(ErrorResponse.create(assetTransferResponse).isError());
        generateBlock();

        JO response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).param("type", "ASSET").param("id", Long.toUnsignedString(myAsset.getAsset())).param("thresholdBalance", 8L).call();
        JA messages = response.getArray("messages");
        Assert.assertEquals(String.format(WhaleAlert.ALERT_MESSAGE_FORMAT, "Testnet", 10.0000, myAsset.getName(), "asset", "transferred", Nxt.getBlockchain().getHeight(), " from ARDOR-XK4R-7VJU-6EQG-7R335 (Alice) to ARDOR-EVHD-5FLM-3NMQ-G46NR"), messages.get(0).getString("message"));
    }

    @Test
    public void assetExchangeAlert() {
        // Issue the asset
        TransactionResponse issueAssetTransaction = IssueAssetCall.create(2).name("myAsset").description("...").quantityQNT(123456789).decimals(4).privateKey(ALICE.getPrivateKey()).feeNQT(100 * ChildChain.IGNIS.ONE_COIN).getCreatedTransaction();
        generateBlock();
        AssetEntityResponse myAsset = AssetEntityResponse.create(GetAssetCall.create().asset(Convert.fullHashToId(issueAssetTransaction.getFullHash())).call());

        // Deploy the contract
        ContractTestHelper.deployContract(SlackNotifier.class);
        String contractName = ContractTestHelper.deployContract(WhaleAlert.class);

        // Place orders
        TransactionResponse bidOrder = PlaceAskOrderCall.create(2).asset(myAsset.getAsset()).quantityQNT(300000).priceNQTPerShare(5000).privateKey(ALICE.getPrivateKey()).feeNQT(ChildChain.IGNIS.ONE_COIN).getCreatedTransaction();
        Logger.logInfoMessage(Convert.toHexString(bidOrder.getFullHash()));
        TransactionResponse askOrder = PlaceBidOrderCall.create(2).asset(myAsset.getAsset()).quantityQNT(100000).priceNQTPerShare(20000).privateKey(BOB.getPrivateKey()).feeNQT(ChildChain.IGNIS.ONE_COIN).getCreatedTransaction();
        Logger.logInfoMessage(Convert.toHexString(askOrder.getFullHash()));
        generateBlock();

        // Check the alert
        JO response = TriggerContractByRequestCall.create().contractName(contractName).setParamValidation(false).param("height", Nxt.getBlockchain().getHeight()).param("type", "ASSET").param("id", Long.toUnsignedString(myAsset.getAsset())).param("thresholdBalance", 8L).call();
        JA messages = response.getArray("messages");
        Assert.assertEquals(String.format(WhaleAlert.ALERT_MESSAGE_FORMAT, "Testnet", 10.0000, myAsset.getName(), "asset", "traded", Nxt.getBlockchain().getHeight(), ""), messages.get(0).getString("message"));
    }

}
