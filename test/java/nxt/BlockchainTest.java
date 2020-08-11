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

package nxt;

import nxt.account.Account;
import nxt.blockchain.BlockchainProcessor;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import nxt.blockchain.TransactionProcessorImpl;
import nxt.blockchain.chaincontrol.PermissionTestUtil;
import nxt.crypto.Crypto;
import nxt.dbschema.Db;
import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.JSONAssert;
import nxt.util.Logger;
import nxt.util.Time;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static nxt.blockchain.chaincontrol.PermissionType.CHAIN_USER;

public abstract class BlockchainTest extends AbstractBlockchainTest {
    @ClassRule
    public static final DoPrivilegedTestRule DO_PRIVILEGED_TEST_RULE = new DoPrivilegedTestRule();

    static {
        System.setProperty(Nxt.NXT_PROPERTIES, "test/java/unit-tests-nxt.properties");
    }

    protected static Tester FORGY;
    public static Tester ALICE;
    public static Tester BOB;
    public static Tester CHUCK;
    public static Tester DAVE;
    protected static Tester RIKER;

    protected static final int baseHeight = 1;

    protected static String forgerSecretPhrase = "aSykrgKGZNlSVOMDxkZZgbTvQqJPGtsBggb";
    protected static final String forgerPublicKey = Convert.toHexString(Crypto.getPublicKey(Crypto.getPrivateKey(forgerSecretPhrase)));

    public static final String aliceSecretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
    public static final String bobSecretPhrase2 = "rshw9abtpsa2";
    public static final String chuckSecretPhrase = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    public static final String daveSecretPhrase = "t9G2ymCmDsQij7VtYinqrbGCOAtDDA3WiNr";
    public static final String rikerSecretPhrase = "5hiig9BPdYoBzWni0QPaCDno6Wz0Vg8oX9yMcXRjEhmkuQKhvB";
    protected static final String rikerPublicKey = Convert.toHexString(Crypto.getPublicKey(Crypto.getPrivateKey(rikerSecretPhrase)));

    protected static boolean isNxtInitialized = false;
    private static boolean isRunInSuite = false;
    private static final Map<String, String> additionalProperties = new HashMap<>();

    public static void setIsRunInSuite(boolean isRunInSuite) {
        BlockchainTest.isRunInSuite = isRunInSuite;
    }

    public static void initNxt(Map<String, String> additionalProperties) {
        if (!isNxtInitialized) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                Properties properties = ManualForgingTest.newTestProperties();
                properties.setProperty("nxt.isTestnet", "true");
                properties.setProperty("nxt.isAutomatedTest", "true");
                properties.setProperty("nxt.isOffline", "true");
                properties.setProperty("nxt.enableFakeForging", "true");
                properties.setProperty("nxt.fakeForgingPublicKeys", forgerPublicKey + ";" + rikerPublicKey);
                properties.setProperty("nxt.timeMultiplier", "1");
                properties.setProperty("nxt.testnetGuaranteedBalanceConfirmations", "1");
                properties.setProperty("nxt.testnetLeasingDelay", "1");
                properties.setProperty("nxt.disableProcessTransactionsThread", "true");
                properties.setProperty("nxt.deleteFinishedShufflings", "false");
                properties.setProperty("nxt.disableAdminPassword", "true");
                properties.setProperty("nxt.testDbDir", "./nxt_unit_test_db/nxt");
                properties.setProperty("nxt.secretPhrasePieces.ARDOR-XK4R-7VJU-6EQG-7R335", "1:9999:3:2:0:2:01d8ce9df0a2bbc29140a56211262d9449d501508b1c5547e5");
                properties.setProperty("nxt.privateKeyPieces.ARDOR-EVHD-5FLM-3NMQ-G46NR", "3:1539292261:3:2:0:1:6d07741e869f03ccd4837d7c33984bc5abf149e6049a498fc8d5a70897ed5838");
                properties.setProperty("nxt.addOns", "nxt.http.CustomSensitiveParameterAddOn;nxt.addons.TaxReportAddOn");
                properties.setProperty("nxt.apiSSL", "false");
                properties.setProperty("nxt.ledgerTrimKeep", "0"); // required by nxt.addons.taxreport.TaxReportAddOnTest
                properties.setProperty("nxt.ledgerLogUnconfirmed", "0"); // required by nxt.addons.taxreport.TaxReportAddOnTest

                additionalProperties.forEach(properties::setProperty);
                AbstractForgingTest.init(properties);
                isNxtInitialized = true;
                return null;
            });
        }
    }

    @BeforeClass
    public static void init() {
        initNxt(additionalProperties);
        initBlockchainTest();
        Assume.assumeThat(Db.PREFIX, CoreMatchers.equalTo("nxt.testDb"));
    }

    @AfterClass
    public static void shutdownNxt() {
        Nxt.getBlockchainProcessor().popOffTo(0);
        if (!isRunInSuite) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                Nxt.shutdown();
                return null;
            });
        }
    }

    protected static void initBlockchainTest() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Nxt.setTime(new Time.CounterTime(Nxt.getEpochTime()));

            giveChainPermissions(forgerSecretPhrase, aliceSecretPhrase, bobSecretPhrase2, chuckSecretPhrase,
                    daveSecretPhrase, rikerSecretPhrase);

            fundTestAccounts();

            FORGY = new Tester(forgerSecretPhrase);
            ALICE = new Tester(aliceSecretPhrase);
            BOB = new Tester  (bobSecretPhrase2);
            CHUCK = new Tester(chuckSecretPhrase);
            DAVE = new Tester (daveSecretPhrase);
            RIKER = new Tester(rikerSecretPhrase);

            Nxt.getBlockchainProcessor().popOffTo(baseHeight);
            Logger.logMessage("baseHeight: " + baseHeight);
            startBundlers();
            return null;
        });
    }

    private static void giveChainPermissions(String... testers) {
        ChildChain.getAll().stream()
                .filter(ChildChain::isEnabled)
                .forEach(chain -> Stream.of(testers)
                        .mapToLong(secretPhrase -> Account.getId(Crypto.getPublicKey(Crypto.getPrivateKey(secretPhrase))))
                        .forEach(accountId -> PermissionTestUtil.grantPermission(chain, accountId, CHAIN_USER, accountId)));
    }

    private static void fundTestAccounts() {
        if (Nxt.getBlockchain().getHeight() == 0) {
            Nxt.getTransactionProcessor().clearUnconfirmedTransactions();

            APICall.Builder sendFxtBuilder = new APICall.Builder("sendMoney").secretPhrase(rikerSecretPhrase).
                    param("chain", "" + FxtChain.FXT.getId()).
                    param("amountNQT", 100_000 * FxtChain.FXT.ONE_COIN).
                    param("feeNQT", FxtChain.FXT.ONE_COIN * 11);

            APICall.Builder sendIgnisBuilder = new APICall.Builder("sendMoney").secretPhrase(rikerSecretPhrase).
                    param("chain", "" + ChildChain.IGNIS.getId()).
                    param("amountNQT", 100_000 * ChildChain.IGNIS.ONE_COIN).
                    param("feeNQT", ChildChain.IGNIS.ONE_COIN * 11);

            APICall.Builder sendAeurBuilder = new APICall.Builder("sendMoney").secretPhrase(rikerSecretPhrase).
                    param("chain", "" + ChildChain.AEUR.getId()).
                    param("amountNQT", 100_000 * ChildChain.AEUR.ONE_COIN).
                    param("feeNQT", ChildChain.AEUR.ONE_COIN * 11);

            List<String> ignisTransactionsToBundle = new ArrayList<>();
            List<String> aeurTransactionsToBundle = new ArrayList<>();
            for (String secret : Arrays.asList(aliceSecretPhrase, bobSecretPhrase2, chuckSecretPhrase, daveSecretPhrase, forgerSecretPhrase)) {
                byte[] publicKey = Crypto.getPublicKey(Crypto.getPrivateKey(secret));
                String publicKeyStr = Convert.toHexString(publicKey);
                String id = Long.toUnsignedString(Account.getId(publicKey));

                sendFxtBuilder.param("recipient", id);
                new JSONAssert(sendFxtBuilder.build().invokeNoError()).str("fullHash");

                sendIgnisBuilder.param("recipient", id).param("recipientPublicKey", publicKeyStr);
                ignisTransactionsToBundle.add(new JSONAssert(sendIgnisBuilder.build().invoke()).str("fullHash"));
                sendAeurBuilder.param("recipient", id);
                aeurTransactionsToBundle.add(new JSONAssert(sendAeurBuilder.build().invoke()).str("fullHash"));
            }

            bundleTransactions(ignisTransactionsToBundle);
            bundleTransactions(aeurTransactionsToBundle);

            try {
                blockchainProcessor.generateBlock(Crypto.getPrivateKey(rikerSecretPhrase), Nxt.getEpochTime());
            } catch (BlockchainProcessor.BlockNotAcceptedException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
    }

    protected static void bundleTransactions(List<String> transactionsToBundle) {
        APICall.Builder builder = new APICall.Builder("bundleTransactions").secretPhrase(rikerSecretPhrase).
                param("chain", "" + FxtChain.FXT.getId()).
                param("transactionFullHash", transactionsToBundle.toArray(new String[0]));

        new JSONAssert(builder.build().invoke()).str("fullHash");
    }

    private static void startBundlers() {
        for (Chain chain : ChildChain.getAll()) {
            long factor = Convert.decimalMultiplier(FxtChain.getChain(1).getDecimals() - chain.getDecimals());
            new APICall.Builder("startBundler").
                    secretPhrase(FORGY.getSecretPhrase()).
                    param("chain", chain.getId()).
                    param("minRateNQTPerFXT", chain.ONE_COIN / factor / 10). // Make it low to allow more transactions
                    param("totalFeesLimitFQT", 20000 * chain.ONE_COIN * factor). // Forgy has only 24K Ignis
                    param("overpayFQTPerFXT", 0).
                    build().invokeNoError();
        }
    }

    @After
    public void destroy() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            TransactionProcessorImpl.getInstance().clearUnconfirmedTransactions();
            blockchainProcessor.popOffTo(baseHeight);
            return null;
        });
    }

    public static void generateBlock() {
        generateBlock(forgerSecretPhrase);
    }

    public static void generateBlockAndSleep() {
        generateBlock();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
        }
    }

    public static void generateBlock(Tester tester) {
        generateBlock(tester.getSecretPhrase());
    }

    private static void generateBlock(String forgerSecretPhrase) {
        Logger.logDebugMessage("vvvvvvvvvvvvvvvvv    generateBlock()    vvvvvvvvvvvvvvvvv");
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                blockchainProcessor.generateBlock(Crypto.getPrivateKey(forgerSecretPhrase), Nxt.getEpochTime());
                return null;
            });
        } catch (PrivilegedActionException e) {
            e.printStackTrace();
            Assert.fail();
        }
        Logger.logDebugMessage("^----------------    generateBlock()    ----------------^");
    }

    protected static void generateBlocks(int howMany) {
        for (int i = 0; i < howMany; i++) {
            generateBlock();
        }
    }

    public static Object putAdditionalProperty(String key, String value) {
        return additionalProperties.put(key, value);
    }
}
