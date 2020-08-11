package nxt.http;

import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.blockchain.ChildChain;
import nxt.http.APICall.InvocationError;
import nxt.http.callers.BundleTransactionsCall;
import nxt.http.callers.SendMessageCall;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.FxtChain.FXT;

public class BundleTransactionsTest extends BlockchainTest {
    ChildChain chain = AEUR;

    @Test
    public void bundleTransactionsExpiringSoon() {

        final String hash1 = (String) sendMessageCall()
                .deadline(5)
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .deadline(6)
                .build().invokeNoError()
                .get("fullHash");


        final JSONObject actual = bundleTransactionsCall(hash1, hash2)
                .build().invokeNoError();

        // deadline is less then minimal transaction expiration because it also adjusts for transaction timestamp difference
        Assert.assertEquals(4, getDeadline(actual));
    }

    @Test
    public void bundleTransactionsExpiringSoonWithTimestampSet() {

        final String hash1 = (String) sendMessageCall()
                .deadline(5)
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .deadline(6)
                .build().invokeNoError()
                .get("fullHash");

        moveTimeForward(3 * 60);


        final JSONObject actual = bundleTransactionsCall(hash1, hash2)
                .timestamp(Nxt.getEpochTime() - 2 * 60)
                .build()
                .invokeNoError();

        // deadline is less then minimal transaction expiration because it also adjusts for transaction timestamp difference
        // due to time move and timestamp applied, now bundling transaction is 1 minute after bundled transactions, so "-1" to deadline.
        Assert.assertEquals(4 - 1, getDeadline(actual));
    }

    @Test
    public void bundleTransactionsExpired() {
        final String hash1 = (String) sendMessageCall()
                .deadline(1)
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .deadline(4)
                .build().invokeNoError()
                .get("fullHash");

        moveTimeForward(2 * 60);


        final JSONObject actual = bundleTransactionsCall(hash1, hash2)
                .build().invokeNoError();


        // deadline is less then minimal transaction expiration because it also adjusts for transaction timestamp difference
        // 2 because of time forward, and -1 to what is left.
        Assert.assertEquals(4 - 2 - 1, getDeadline(actual));
        final JA submittedTransactions = new JO(actual).getJo("transactionJSON").getJo("attachment").getArray("childTransactionFullHashes");
        Assert.assertEquals(Collections.singletonList(hash2), submittedTransactions);
    }

    // don't know if I should fix this case. Looks rare. Unclear how to fix.
    @Ignore
    @Test
    public void bundleTransactionsExpiredDueToRounding() {
        final String hash1 = (String) sendMessageCall()
                .deadline(1)
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .deadline(3)
                .build().invokeNoError()
                .get("fullHash");

        moveTimeForward(2 * 60);

        final InvocationError actual = bundleTransactionsCall(hash1, hash2)
                .build().invokeWithError();
        assertEmptyTransactionsListFailure(actual);
    }

    @Test
    public void bundleTransactionsAllExpired() {
        final String hash1 = (String) sendMessageCall()
                .deadline(1)
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .deadline(1)
                .build().invokeNoError()
                .get("fullHash");

        moveTimeForward(60);

        final InvocationError actual = bundleTransactionsCall(hash1, hash2)
                .build().invokeWithError();

        assertEmptyTransactionsListFailure(actual);
    }

    /**
     * See nxt.util.Time.CounterTime
     */
    private void moveTimeForward(int seconds) {
        for (int i = 0; i < seconds; i++) {
            Nxt.getEpochTime();
        }
    }

    @Test
    public void bundleTransactionsExpiringInDistantFuture() {

        final String hash1 = (String) sendMessageCall()
                .deadline(14400)
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .build().invokeNoError()
                .get("fullHash");


        final JSONObject actual = bundleTransactionsCall(hash1, hash2)
                .build().invokeNoError();

        Assert.assertEquals(10, getDeadline(actual));
    }

    private BundleTransactionsCall bundleTransactionsCall(String... hashes) {
        final BundleTransactionsCall builder = BundleTransactionsCall.create(FXT.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .feeNQT(IGNIS.ONE_COIN);
        if (hashes.length == 0) {
            return builder;
        }
        return builder.transactionFullHash(hashes);
    }

    private SendMessageCall sendMessageCall() {
        return SendMessageCall.create(chain.getId())
                .secretPhrase(ALICE.getSecretPhrase())
                .recipient(BOB.getStrId())
                .feeNQT(chain.ONE_COIN);
    }

    private int getDeadline(JSONObject actual) {
        return new JO(actual).getJo("transactionJSON").getInt("deadline");
    }

    @Test
    public void bundleTransactions() {

        final String hash1 = (String) sendMessageCall()
                .build().invokeNoError()
                .get("fullHash");

        final String hash2 = (String) sendMessageCall()
                .build().invokeNoError()
                .get("fullHash");


        final JSONObject actual = bundleTransactionsCall(hash1, hash2)
                .deadline(10)
                .build()
                .invokeNoError();

        Assert.assertEquals(10, getDeadline(actual));
    }

    @Test
    public void bundleTransactionsEmptyList() {
        final InvocationError actual = bundleTransactionsCall()
                .deadline(10)
                .build()
                .invokeWithError();

        Assert.assertEquals("\"transactionFullHash\" not specified", actual.getErrorDescription());
        Assert.assertEquals(3, actual.getErrorCode());
    }

    private void assertEmptyTransactionsListFailure(InvocationError actual) {
        Assert.assertEquals("Empty ChildBlockAttachment not allowed", actual.getErrorDescription());
        Assert.assertEquals(4, actual.getErrorCode());
    }
}