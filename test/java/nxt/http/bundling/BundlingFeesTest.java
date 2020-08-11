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

package nxt.http.bundling;

import nxt.Constants;
import nxt.Nxt;
import nxt.account.PaymentTransactionType;
import nxt.http.APICall;
import nxt.http.callers.BundleTransactionsCall;
import nxt.http.callers.GetBundlersCall;
import nxt.http.callers.GetTransactionCall;
import nxt.http.callers.GetUnconfirmedTransactionsCall;
import nxt.http.callers.SendMoneyCall;
import nxt.http.callers.StartBundlerCall;
import nxt.util.JSONAssert;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.FxtChain.FXT;

public class BundlingFeesTest extends BundlerTest {
    @Test
    public void testCurrentFeesRestore() {
        startBundler();

        String sendMoneyFullHash = sendMoney();

        APICall getChildTxCall = GetTransactionCall.create(IGNIS.getId())
                .fullHash(sendMoneyFullHash)
                .build();
        JSONAssert jsonAssert = new JSONAssert(getChildTxCall.invokeNoError());
        Assert.assertEquals(true, jsonAssert.bool("isBundled"));

        JSONAssert childBlockTx = getForgyUnconfirmedFxtTransaction();

        int childBlockExpiration = (int) (childBlockTx.integer("timestamp") + childBlockTx.integer("deadline") * 60);

        //for the one transaction with the default MIN_FEE calculator
        long feePayedByForgy = PaymentTransactionType.ORDINARY.getBaselineFee(null)
                .getFee(null, null);
        long feePayedByChuck = feePayedByForgy + 1;
        Assert.assertEquals(feePayedByForgy, getCurrentTotalFees());

        new JSONAssert(BundleTransactionsCall.create(FXT.getId())
                .secretPhrase(CHUCK.getSecretPhrase())
                .feeNQT(feePayedByChuck)
                .deadline(10)
                .transactionFullHash(sendMoneyFullHash).build().invokeNoError());

        generateBlock();

        Assert.assertEquals("Chuck must have bundled the transaction",
                -feePayedByForgy - 1, CHUCK.getFxtBalanceDiff());

        Assert.assertTrue(Nxt.getEpochTime() + Constants.MAX_TIMEDRIFT < childBlockExpiration);
        Assert.assertEquals("FORGY's current total fees still not restored", feePayedByForgy, getCurrentTotalFees());

        while (Nxt.getEpochTime() + Constants.MAX_TIMEDRIFT < childBlockExpiration) {
            //empty
        }

        generateBlock();

        Assert.assertEquals("FORGY's current total fees must be restored", 0, getCurrentTotalFees());
    }

    @Test
    public void testCurrentFees() {
        startBundler();
        sendMoney();

        JSONAssert childBlockTx = getForgyUnconfirmedFxtTransaction();

        int childBlockExpiration = (int) (childBlockTx.integer("timestamp") + childBlockTx.integer("deadline") * 60 + 60);

        while (Nxt.getEpochTime() < childBlockExpiration) {
            //empty
        }

        generateBlock();

        Assert.assertEquals("FORGY's current total fees must not be restored", 0, getCurrentTotalFees());
    }

    private void startBundler() {
        StartBundlerCall.create(IGNIS.getId())
                .secretPhrase(FORGY.getSecretPhrase())
                .minRateNQTPerFXT((101 * IGNIS.ONE_COIN) / 100)
                .totalFeesLimitFQT(100 * IGNIS.ONE_COIN)
                .build().invokeNoError();
    }

    private String sendMoney() {
        JSONAssert sendMoneyResult = new JSONAssert(SendMoneyCall.create(IGNIS.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .amountNQT(12 * IGNIS.ONE_COIN)
                .recipient(BOB.getId())
                .deadline(20)
                .feeNQT(IGNIS.ONE_COIN).build().invokeNoError());

        return sendMoneyResult.fullHash();
    }

    private JSONAssert getForgyUnconfirmedFxtTransaction() {
        JSONAssert jsonAssert = new JSONAssert(GetUnconfirmedTransactionsCall.create(FXT.getId())
                .account(FORGY.getId()).build().invokeNoError());
        return new JSONAssert(jsonAssert.array("unconfirmedTransactions", JSONObject.class).get(0));
    }


    private long getCurrentTotalFees() {
        JSONAssert getBundlersResult = new JSONAssert(GetBundlersCall.create(IGNIS.getId())
                .account(FORGY.getId()).build().invokeNoError());
        return Long.parseUnsignedLong(new JSONAssert(getBundlersResult
                .array("bundlers", JSONObject.class).get(0))
                .str("currentTotalFeesFQT"));
    }
}
