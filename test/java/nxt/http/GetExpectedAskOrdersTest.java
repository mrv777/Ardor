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

package nxt.http;

import nxt.BlockchainTest;
import nxt.Tester;
import nxt.addons.JO;
import nxt.blockchain.ChildChain;
import nxt.http.callers.GetExpectedAskOrdersCall;
import nxt.http.callers.IssueAssetCall;
import nxt.http.callers.PlaceAskOrderCall;
import nxt.http.callers.PlaceBidOrderCall;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nxt.blockchain.ChildChain.IGNIS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GetExpectedAskOrdersTest extends BlockchainTest {
    private final Tester tester = ALICE;
    private final ChildChain chain = IGNIS;
    private int assetNumber = 0;


    @Test
    public void testGetAskOrdersBundledAndNot() {
        long assetId = createAsset();
        generateBlock();

        String confirmedOrder = placeAskOrder(assetId);

        generateBlock();
        String expectedOrder = placeAskOrder(assetId);

        final JSONObject result = getExpectedAskOrdersCall().asset(assetId).build().invokeNoError();

        assertContainsOrder("expectedOrder", expectedOrder, result);
        assertExcludesOrder("confirmedOrder", confirmedOrder, result);
    }

    @Test
    public void testGetAskOrders() {
        long assetId = createAsset();
        generateBlock();

        String order = placeAskOrder(assetId);

        final JSONObject result = getExpectedAskOrdersCall().asset(assetId).build().invokeNoError();

        assertContainsOrder("expectedOrder", order, result);
    }

    @Test
    public void testGetAskAndBidOrders() {
        long assetId = createAsset();
        generateBlock();

        String askOrder = placeAskOrder(assetId);
        String bidOrder = placeBidOrder(assetId);

        final JSONObject result = getExpectedAskOrdersCall().asset(assetId).build().invokeNoError();

        assertContainsOrder("askOrder", askOrder, result);
        assertExcludesOrder("bidOrder", bidOrder, result);
    }

    @Test
    public void testGetAskOrdersOneAsset() {
        long asset = createAsset();
        long assetAnother = createAsset();
        generateBlock();

        String order = placeAskOrder(asset);
        String orderAnother = placeAskOrder(assetAnother);


        final JSONObject result = getExpectedAskOrdersCall().asset(asset).build().invokeNoError();

        assertContainsOrder("order from asset", order, result);
        assertExcludesOrder("order from another asset", orderAnother, result);
    }

    private GetExpectedAskOrdersCall getExpectedAskOrdersCall() {
        return GetExpectedAskOrdersCall.create(chain.getId());
    }

    Map<String, JO> getOrders(JSONObject response) {
        return new JO(response)
                .getJoList("askOrders")
                .stream()
                .collect(Collectors.toMap(j -> j.getString("orderFullHash"), Function.identity()));

    }

    private void assertContainsOrder(String comment, String orderHash, JSONObject actual) {
        final Set<String> actualOrderHashes = getOrders(actual).keySet();
        assertTrue(comment + " is missing. Expected hash " + orderHash + ", actual hashes: " + actualOrderHashes, actualOrderHashes.contains(orderHash));
    }

    private void assertExcludesOrder(String comment, String orderHash, JSONObject actual) {
        final Set<String> actualOrderHashes = getOrders(actual).keySet();
        assertFalse(comment + " is present. Expected hash " + orderHash + " to be not present, actual hashes: " + actualOrderHashes, actualOrderHashes.contains(orderHash));
    }

    private String placeAskOrder(long assetId) {
        return new JO(PlaceAskOrderCall.create(chain.getId())
                .asset(assetId)
                .secretPhrase(tester.getSecretPhrase())
                .priceNQTPerShare(2)
                .quantityQNT(1)
                .feeNQT(chain.ONE_COIN)
                .build()
                .invokeNoError())
                .getString("fullHash");
    }

    private String placeBidOrder(long assetId) {
        return new JO(PlaceBidOrderCall.create(chain.getId())
                .asset(assetId)
                .secretPhrase(tester.getSecretPhrase())
                .priceNQTPerShare(1)
                .quantityQNT(1)
                .feeNQT(chain.ONE_COIN)
                .build()
                .invokeNoError())
                .getString("fullHash");
    }

    private long createAsset() {
        final JSONObject callResult = IssueAssetCall.create(chain.getId())
                .name("Asks" + ++assetNumber)
                .decimals(0)
                .quantityQNT(100000)
                .secretPhrase(tester.getSecretPhrase())
                .feeNQT(10 * chain.ONE_COIN)
                .build()
                .invokeNoError();
        final long result = Long.parseUnsignedLong(Tester.responseToStringId(callResult));
        generateBlock();
        return result;
    }

}