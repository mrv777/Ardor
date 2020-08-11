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
package nxt.peer;

import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.Tester;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.http.callers.SendMoneyCall;
import nxt.util.JSONAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.ChildChain.MPG;
import static nxt.peer.FeeRateCalculator.TransactionPriority.HIGH;
import static nxt.peer.FeeRateCalculator.TransactionPriority.LOW;
import static nxt.peer.FeeRateCalculator.TransactionPriority.NORMAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FeeRateCalculatorTest extends BlockchainTest {

    public static final long MPG_RATE = 3 * MPG.ONE_COIN + MPG.ONE_COIN / 2;

    @Test
    public void testBestRates() {
        createTestRates();

        FeeRateCalculator feeRateCalculator = FeeRateCalculator.create().setPriority(HIGH).build();
        List<BundlerRate> bestRates = feeRateCalculator.getBestRates();

        Assert.assertEquals(2, bestRates.size());
        bestRates.forEach(rate -> {
            if (IGNIS.equals(rate.getChain())) {
                assertEquals(3 * IGNIS.ONE_COIN, rate.getRate());
            } else if (MPG.equals(rate.getChain())) {
                assertEquals(MPG_RATE, rate.getRate());
            } else {
                fail();
            }
        });
    }

    @Test
    public void testBestRate() {
        createTestRates();
        FeeRateCalculator.Builder builder = new FeeRateCalculator.Builder();

        Assert.assertEquals(IGNIS.ONE_COIN, builder.setPriority(LOW).build().getBestRate(IGNIS));
        Assert.assertEquals(2 * IGNIS.ONE_COIN, builder.setPriority(NORMAL).build().getBestRate(IGNIS));
        Assert.assertEquals(3 * IGNIS.ONE_COIN, builder.setPriority(HIGH).build().getBestRate(IGNIS));
        Assert.assertEquals(MPG_RATE, builder.build().getBestRate(MPG));
    }

    @Test
    public void testPriorityParameter() {
        createTestRates();

        SendMoneyCall sendMoneyCall = SendMoneyCall.create(IGNIS.getId()).amountNQT(IGNIS.ONE_COIN).recipient(BOB.getId())
                .transactionPriority("HIGH")
                .secretPhrase(ALICE.getSecretPhrase());
        Assert.assertEquals(3 * IGNIS.ONE_COIN, invokeFeeCalculation(sendMoneyCall));
        sendMoneyCall.transactionPriority("2");
        Assert.assertEquals(3 * IGNIS.ONE_COIN, invokeFeeCalculation(sendMoneyCall));

        sendMoneyCall.transactionPriority("LOW");
        Assert.assertEquals(IGNIS.ONE_COIN, invokeFeeCalculation(sendMoneyCall));
        sendMoneyCall.transactionPriority("0");
        Assert.assertEquals(IGNIS.ONE_COIN, invokeFeeCalculation(sendMoneyCall));
    }

    @After
    public void destroy() {
        super.destroy();
        int ratesExpiration = Nxt.getEpochTime() + Peers.BUNDLER_RATE_BROADCAST_INTERVAL + 15 * 60;
        while (Nxt.getEpochTime() < ratesExpiration) {
            //empty
        }
        Assert.assertEquals(0, Peers.getAllBundlerRates(0).size());
    }

    private long invokeFeeCalculation(SendMoneyCall sendMoneyCall) {
        return new JSONAssert(sendMoneyCall.build().invokeNoError()).amount("bundlerRateNQTPerFXT");
    }

    private void createTestRates() {
        ArrayList<BundlerRate> rates = new ArrayList<>();
        rates.add(createRate(IGNIS, ALICE, IGNIS.ONE_COIN));
        rates.add(createRate(IGNIS, BOB, 2 * IGNIS.ONE_COIN));
        rates.add(createRate(IGNIS, CHUCK, 3 * IGNIS.ONE_COIN));
        rates.add(createRate(IGNIS, DAVE, 4 * IGNIS.ONE_COIN));
        rates.add(createRate(MPG, FORGY, MPG_RATE));
        Peers.updateBundlerRates(new DummyPeer(), null, rates);
    }

    private BundlerRate createRate(ChildChain childChain, Tester bundlerAccount, long rate) {
        BundlerRate bundlerRate = new BundlerRate(childChain, rate, 100 * FxtChain.FXT.ONE_COIN, bundlerAccount.getPrivateKey());
        bundlerRate.setBalance(bundlerAccount.getChainBalance(childChain.getId()));
        return bundlerRate;
    }

}