/*
 * Copyright © 2020 Jelurida IP B.V.
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
package nxt.http.coinexchange;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.addons.JO;
import nxt.ce.CoinExchangeFxtTransactionType;
import nxt.ce.CoinExchangeTransactionType;
import nxt.http.APICall;
import nxt.http.callers.BroadcastTransactionCall;
import nxt.http.callers.CancelCoinExchangeCall;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.callers.GetCoinExchangeOrderCall;
import nxt.http.callers.SignTransactionCall;
import nxt.util.JSONAssert;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.FxtChain.FXT;

public class CancelOrderTest extends BlockchainTest {
    @Test
    public void testCancelFxtOrderOnChildChain() {
        String orderId = createFxtOrder().id();
        JSONObject transactionJSON = createCancellingOrderOnIgnis(orderId);
        signAndBroadcast(transactionJSON, true);

        //check that the order was canceled
        checkOrderCall(orderId).invokeWithError();

        generateBlocks(Constants.CANCEL_FXT_COIN_ORDER_FIX_BLOCK);

        //try the same after CANCEL_FXT_COIN_ORDER_FIX_BLOCK, without validation during sign
        orderId = createFxtOrder().id();
        transactionJSON = createCancellingOrderOnIgnis(orderId);
        JO broadcastResult = signAndBroadcast(transactionJSON, false);

        Assert.assertTrue(broadcastResult.getString("error").
                contains("Cancelling parent chain coin exchanges must be submitted on parent chain"));
    }

    private JO signAndBroadcast(JSONObject transactionJSON, boolean validate) {
        JSONAssert signResult = new JSONAssert(SignTransactionCall.create().unsignedTransactionJSON(transactionJSON.toJSONString()).
                    validate(validate).secretPhrase(ALICE.getSecretPhrase()).build().invokeNoError());

        JO result = BroadcastTransactionCall.create().transactionJSON(signResult.subObj("transactionJSON").getJson().toJSONString()).call();
        generateBlock();
        return result;
    }

    private JSONAssert createFxtOrder() {
        JSONAssert result = new JSONAssert(ExchangeCoinsCall.create(IGNIS.getId()).exchange(FXT.getId()).quantityQNT(100).
                priceNQTPerCoin(IGNIS.ONE_COIN * 2).feeRateNQTPerFXT(IGNIS.ONE_COIN).
                secretPhrase(ALICE.getSecretPhrase()).build().invokeNoError());
        Assert.assertEquals("ARDR exchanges are automatically created on the ARDR chain",
                FXT.getId(), result.subObj("transactionJSON").integer("chain"));

        generateBlock();
        return result;
    }

    private APICall checkOrderCall(String orderId) {
        return GetCoinExchangeOrderCall.create().order(orderId).build();
    }

    private JSONObject createCancellingOrderOnIgnis(String orderId) {
        JSONAssert cancelRes = new JSONAssert(CancelCoinExchangeCall.create(IGNIS.getId()).order(orderId).
                publicKey(ALICE.getPublicKey()).broadcast(false).build().invokeNoError());

        generateBlock();

        //check that the order is still here
        checkOrderCall(orderId).invokeNoError();

        Assert.assertEquals("Cancel ARDR orders are automatically created on the ARDR chain",
                FXT.getId(), cancelRes.subObj("transactionJSON").integer("chain"));

        //manually switch the transaction chain to IGNIS
        JSONObject transactionJSON = cancelRes.subObj("transactionJSON").getJson();
        transactionJSON.put("chain", IGNIS.getId());
        transactionJSON.put("type", CoinExchangeTransactionType.ORDER_CANCEL.getType());
        transactionJSON.put("subtype", CoinExchangeTransactionType.ORDER_CANCEL.getSubtype());
        JSONObject attachment = (JSONObject) transactionJSON.get("attachment");
        attachment.remove("version." + CoinExchangeFxtTransactionType.ORDER_CANCEL.getName());
        attachment.put("version." + CoinExchangeTransactionType.ORDER_CANCEL.getName(), 1);
        return transactionJSON;
    }



}
