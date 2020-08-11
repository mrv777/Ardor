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

package nxt.http.assetexchange;

import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.RequireNonePermissionPolicyTestsCategory;
import nxt.Tester;
import nxt.account.HoldingType;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.http.APICall;
import nxt.http.APICall.InvocationError;
import nxt.http.client.IssueAssetBuilder;
import nxt.http.client.IssueAssetBuilder.IssueAssetResult;
import nxt.http.client.PlaceAssetOrderBuilder;
import nxt.http.client.TransferAssetBuilder;
import nxt.http.client.TransferAssetBuilder.TransferResult;
import nxt.http.monetarysystem.TestCurrencyIssuance;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.util.Convert.unitRateToAmount;


// since all tests try to pay dividends.
@Category(RequireNonePermissionPolicyTestsCategory.class)
public class AssetExchangeTest extends BlockchainTest {

    public static IssueAssetResult issueAsset(Tester creator, String name) {
        IssueAssetResult result = new IssueAssetBuilder(creator, name).issueAsset();
        BlockchainTest.generateBlock();
        return result;
    }

    static TransferResult transfer(String assetId, Tester from, Tester to, long quantityQNT) {
        return transfer(assetId, from, to, quantityQNT, IGNIS.ONE_COIN);
    }

    public static TransferResult transfer(String assetId, Tester from, Tester to, long quantityQNT, long fee) {
        TransferResult result = new TransferAssetBuilder(assetId, from, to)
                .setQuantityQNT(quantityQNT)
                .setFee(fee)
                .transfer();
        BlockchainTest.generateBlock();
        return result;
    }

    static JSONObject payDividend(String assetId, Tester assetIssuer, int height, long amountNQTPerShare, Chain chain, byte holdingType, String holding) {
        APICall apiCall = payDividendCall(assetId, assetIssuer, height, amountNQTPerShare, chain, holdingType, holding);
        JSONObject response = apiCall.invokeNoError();
        BlockchainTest.generateBlock();
        return response;
    }

    private static APICall payDividendCall(String assetId, Tester assetIssuer, int height, long amountNQTPerShare, Chain chain, byte holdingType, String holding) {
        return new APICall.Builder("dividendPayment")
                .param("secretPhrase", assetIssuer.getSecretPhrase())
                .param("asset", assetId)
                .param("height", height)
                .param("holdingType", holdingType)
                .param("holding", holding)
                .param("amountNQTPerShare", amountNQTPerShare)
                .param("feeNQT", chain.ONE_COIN)
                .chain(chain.getId())
                .build();
    }

    static InvocationError failToPayDividend(String assetId, Tester assetIssuer, int height, long amountNQTPerShare, Chain chain, byte holdingType, String holding) {
        return payDividendCall(assetId, assetIssuer, height, amountNQTPerShare, chain, holdingType, holding).invokeWithError();
    }

    @Test
    public void ignisDividend() {
        String assetId = issueAsset(ALICE, "divSender").getAssetIdString();
        transfer(assetId, ALICE, BOB, 300 * 10000);
        transfer(assetId, ALICE, CHUCK, 200 * 10000);
        transfer(assetId, ALICE, DAVE, 100 * 10000);
        generateBlock();

        // Pay dividend in IGNIS, nice and round
        int chainId = IGNIS.getId();
        payDividend(assetId, ALICE, Nxt.getBlockchain().getHeight(), 1000000L, IGNIS, HoldingType.COIN.getCode(), "");
        generateBlock();
        Assert.assertEquals(3 * IGNIS.ONE_COIN, BOB.getChainBalanceDiff(chainId));
        Assert.assertEquals(2 * IGNIS.ONE_COIN, CHUCK.getChainBalanceDiff(chainId));
        Assert.assertEquals(IGNIS.ONE_COIN, DAVE.getChainBalanceDiff(chainId));
    }

    @Test
    public void AEURDividend() {
        String assetId = issueAsset(RIKER, "divSender").getAssetIdString();
        transfer(assetId, RIKER, BOB, 5555555);
        transfer(assetId, RIKER, CHUCK, 2222222);
        transfer(assetId, RIKER, DAVE, 1111111);
        generateBlock();

        int chainId = ChildChain.AEUR.getId();
        payDividend(assetId, RIKER, Nxt.getBlockchain().getHeight(), 1L, ChildChain.AEUR, HoldingType.COIN.getCode(), "");
        generateBlock();
        Assert.assertEquals(-555 - 222 - 111 - ChildChain.AEUR.ONE_COIN, RIKER.getChainBalanceDiff(chainId));
        Assert.assertEquals(555, BOB.getChainBalanceDiff(chainId));
        Assert.assertEquals(222, CHUCK.getChainBalanceDiff(chainId));
        Assert.assertEquals(111, DAVE.getChainBalanceDiff(chainId));
    }

    @Test
    public void assetDividend() {
        String assetId = issueAsset(RIKER, "divSender").getAssetIdString();
        transfer(assetId, RIKER, BOB, 5555555);
        transfer(assetId, RIKER, CHUCK, 2222222);
        transfer(assetId, RIKER, DAVE, 1111111);
        IssueAssetResult receiverId = issueAsset(RIKER, "divRecv");
        generateBlock();

        payDividend(assetId, RIKER, Nxt.getBlockchain().getHeight(), 1L, ChildChain.AEUR, HoldingType.ASSET.getCode(), receiverId.getAssetIdString());
        generateBlock();
        Assert.assertEquals(10000000 - 555 - 222 - 111, RIKER.getAssetQuantityDiff(receiverId.getAssetId()));
        Assert.assertEquals(555, BOB.getAssetQuantityDiff(receiverId.getAssetId()));
        Assert.assertEquals(222, CHUCK.getAssetQuantityDiff(receiverId.getAssetId()));
        Assert.assertEquals(111, DAVE.getAssetQuantityDiff(receiverId.getAssetId()));
    }

    @Test
    public void currencyDividend() {
        String assetId = issueAsset(ALICE, "divSender").getAssetIdString();
        transfer(assetId, ALICE, BOB, 5555555);
        transfer(assetId, ALICE, CHUCK, 2222222);
        transfer(assetId, ALICE, DAVE, 1111111);
        String currencyId = new TestCurrencyIssuance().issueCurrencyImpl();
        generateBlock();

        payDividend(assetId, ALICE, Nxt.getBlockchain().getHeight(), 1L, ChildChain.AEUR, HoldingType.CURRENCY.getCode(), currencyId);
        generateBlock();
        Assert.assertEquals(100000 - 555 - 222 - 111, ALICE.getCurrencyUnitsDiff(Long.parseUnsignedLong(currencyId)));
        Assert.assertEquals(555, BOB.getCurrencyUnitsDiff(Long.parseUnsignedLong(currencyId)));
        Assert.assertEquals(222, CHUCK.getCurrencyUnitsDiff(Long.parseUnsignedLong(currencyId)));
        Assert.assertEquals(111, DAVE.getCurrencyUnitsDiff(Long.parseUnsignedLong(currencyId)));
    }

    @Test
    public void tradeAsset() {
        String assetId = issueAsset(ALICE, "divSender").getAssetIdString();

        long fee = IGNIS.ONE_COIN;

        new PlaceAssetOrderBuilder(ALICE, assetId, 10, IGNIS.ONE_COIN * 2)
                .setFeeNQT(fee)
                .placeAskOrder();

        generateBlock();

        new PlaceAssetOrderBuilder(BOB, assetId, 10, IGNIS.ONE_COIN * 2)
                .setFeeNQT(fee)
                .placeBidOrder();

        generateBlock();

        Assert.assertEquals(10, BOB.getAssetQuantityDiff(Long.parseUnsignedLong(assetId)));
        Assert.assertEquals(IssueAssetBuilder.ASSET_QNT - 10, ALICE.getAssetQuantityDiff(Long.parseUnsignedLong(assetId)));

        long sellVolume = unitRateToAmount(10, IssueAssetBuilder.ASSET_DECIMALS, 2 * IGNIS.ONE_COIN, IGNIS.getDecimals());
        Assert.assertEquals(- sellVolume - fee, BOB.getChainBalanceDiff(IGNIS.getId()));
        Assert.assertEquals(sellVolume - fee - IssueAssetBuilder.ASSET_ISSUE_FEE_NQT, ALICE.getChainBalanceDiff(IGNIS.getId()));
    }
}
