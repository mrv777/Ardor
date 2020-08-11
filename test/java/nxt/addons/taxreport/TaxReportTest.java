package nxt.addons.taxreport;

import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.Tester;
import nxt.account.AccountLedger;
import nxt.account.PaymentFxtTransactionType;
import nxt.account.PaymentTransactionType;
import nxt.addons.taxreport.TaxReport.UnknownEventPolicy;
import nxt.blockchain.Block;
import nxt.blockchain.ChildBlockFxtTransactionType;
import nxt.blockchain.Transaction;
import nxt.ce.CoinExchangeTransactionType;
import nxt.http.accountControl.PhasingOnlyTest;
import nxt.http.assetexchange.AssetExchangeTest;
import nxt.http.callers.ScanCall;
import nxt.http.callers.SendMessageCall;
import nxt.http.callers.SendMoneyCall;
import nxt.http.client.IssueAssetBuilder;
import nxt.http.client.PlaceAssetOrderBuilder;
import nxt.http.client.TransferAssetBuilder;
import nxt.http.coinexchange.CoinExchangeTest;
import nxt.http.monetarysystem.TestCurrencyExchange;
import nxt.http.monetarysystem.TestCurrencyIssuance;
import nxt.util.Listener;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static nxt.account.AccountLedger.LedgerEvent.ASSET_TRADE;
import static nxt.account.AccountLedger.LedgerEvent.COIN_EXCHANGE_TRADE;
import static nxt.account.AccountLedger.LedgerEvent.CURRENCY_EXCHANGE;
import static nxt.account.AccountLedger.LedgerEvent.TRANSACTION_FEE;
import static nxt.addons.taxreport.Record.dividend;
import static nxt.addons.taxreport.Record.dividendIncome;
import static nxt.addons.taxreport.Record.income;
import static nxt.addons.taxreport.Record.issueAsset;
import static nxt.addons.taxreport.Record.issueCurrency;
import static nxt.addons.taxreport.Record.mining;
import static nxt.addons.taxreport.Record.record;
import static nxt.addons.taxreport.Record.spend;
import static nxt.addons.taxreport.Record.trade;
import static nxt.addons.taxreport.TransactionsRule.assetName;
import static nxt.addons.taxreport.TransactionsRule.recipient;
import static nxt.addons.taxreport.TransactionsRule.sender;
import static nxt.addons.taxreport.TransactionsRule.type;
import static nxt.ae.AssetExchangeTransactionType.ASSET_ISSUANCE;
import static nxt.ae.AssetExchangeTransactionType.ASSET_TRANSFER;
import static nxt.ae.AssetExchangeTransactionType.BID_ORDER_PLACEMENT;
import static nxt.ae.AssetExchangeTransactionType.DIVIDEND_PAYMENT;
import static nxt.blockchain.BlockchainProcessor.Event.RESCAN_BEGIN;
import static nxt.blockchain.BlockchainProcessor.Event.RESCAN_END;
import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.FxtChain.FXT;
import static nxt.http.client.IssueAssetBuilder.ASSET_ISSUE_FEE_NQT;
import static nxt.http.monetarysystem.TestCurrencyIssuance.Builder.initialSupplyQNT;
import static nxt.messaging.MessagingTransactionType.ARBITRARY_MESSAGE;
import static nxt.ms.MonetarySystemTransactionType.CURRENCY_ISSUANCE;
import static nxt.ms.MonetarySystemTransactionType.CURRENCY_TRANSFER;
import static nxt.ms.MonetarySystemTransactionType.PUBLISH_EXCHANGE_OFFER;
import static nxt.util.Convert.unitRateToAmount;
import static nxt.voting.VotingTransactionType.PHASING_VOTE_CASTING;
import static org.junit.Assert.assertEquals;

public class TaxReportTest extends BlockchainTest {
    private static final String ASSET_NAME = "AssetT";
    private static final int ASSET_DECIMALS = IssueAssetBuilder.ASSET_DECIMALS;
    private static final int decimalsTest1 = 0;
    private static final int decimalsDivSender = IssueAssetBuilder.ASSET_DECIMALS;

    private static final TestListener testListener = new TestListener();
    @Rule
    public final TransactionsRule transactionsRule = new TransactionsRule();
    private final String currencyDisplayName = "TSXXX@IGNIS"; // see nxt.http.monetarysystem.TestCurrencyIssuance.Builder.Builder

    @Before
    public void setUp() {
        generateBlocks(2);

        AccountLedger.addListener(testListener.getAccountLedger(), AccountLedger.Event.ADD_ENTRY);
        Nxt.getBlockchainProcessor().addListener(testListener.getOnRescanBegin(), RESCAN_BEGIN);
        Nxt.getBlockchainProcessor().addListener(testListener.getOnRescanEnd(), RESCAN_END);
    }

    @After
    public void tearDown() {
        AccountLedger.removeListener(testListener.getAccountLedger(), AccountLedger.Event.ADD_ENTRY);
        Nxt.getBlockchainProcessor().removeListener(testListener.getOnRescanBegin(), RESCAN_BEGIN);
        Nxt.getBlockchainProcessor().removeListener(testListener.getOnRescanEnd(), RESCAN_END);
    }

    private static class TestListener {
        private volatile String comment = "Before scan";
        private final Listener<Block> onRescanBeginListener = block -> comment = "In scan";
        private final Listener<Block> onRescanEndListener = block -> comment = "After scan";
        private final Listener<AccountLedger.LedgerEntry> ledgerEntryListener = ledgerEntry -> {
/*
            System.out.printf("*%s* event: %s,\tholding: %s,\tchange: %s,\taccountId: %s\tledgerId: %s,\teventID: %s,\tts: %s%n"
                    , comment
                    , ledgerEntry.getEvent()
                    , ledgerEntry.getHolding()
                    , ledgerEntry.getChange()
                    , Convert.rsAccount(ledgerEntry.getAccountId())
                    , ledgerEntry.getLedgerId()
                    , ledgerEntry.getEventId()
                    , ledgerEntry.getTimestamp()
            );
*/
        };

        private Listener<AccountLedger.LedgerEntry> getAccountLedger() {
            return ledgerEntryListener;
        }

        private Listener<Block> getOnRescanBegin() {
            return onRescanBeginListener;
        }

        private Listener<Block> getOnRescanEnd() {
            return onRescanEndListener;
        }
    }

    @Test
    public void sendAeur() {
        generateBlock();
        int height = getHeight();

        sendAeur(BOB, ALICE, 1000);
        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));

        generateBlock();

        List<Record> actual = runTaxReport(height, BOB);
        assertEquals(
                singletonList(spend().sell(1000, AEUR).fee(1, AEUR).commentAndGroup(sendMoneyId)),
                actual);
    }

    @Test
    public void sendArdor() {
        generateBlock();
        int height = getHeight();

        sendArdor(BOB, ALICE, 1000);

        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentFxtTransactionType.ORDINARY));

        generateBlock();

        assertEquals(
                singletonList(spend().sell(1000, FXT).fee(FXT.ONE_COIN, FXT).commentAndGroup(sendMoneyId)),
                runTaxReport(height, BOB));

        assertEquals(
                singletonList(income().buy(1000, FXT).commentAndGroup(sendMoneyId)),
                runTaxReport(height, ALICE));
    }

    private void sendArdor(Tester from, Tester to, int amountNQT) {
        SendMoneyCall.create(FXT.getId())
                .secretPhrase(from.getSecretPhrase())
                .recipient(to.getId())
                .amountNQT(amountNQT)
                .feeNQT(FXT.ONE_COIN)
                .build()
                .invokeNoError();
        generateBlock();
    }

    @Test
    public void sendMessage() {
        generateBlock();
        int height = getHeight();

        sendMessage(BOB, ALICE);
        final Transaction sendMessageId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(ARBITRARY_MESSAGE));

        generateBlock();

        assertEquals(
                singletonList(record("ARBITRARY_MESSAGE").sell(1, AEUR).comment(sendMessageId).group(TRANSACTION_FEE)),
                runTaxReport(height, BOB));
        assertEquals(emptyList(), runTaxReport(height, ALICE));
    }

    @Test
    public void sendMoneyReportForMultipleAccounts() {
        generateBlock();
        int height = getHeight();

        sendAeur(BOB, ALICE, 1000);

        final Transaction generateBlockId = transactionsRule.findOnly(type(ChildBlockFxtTransactionType.INSTANCE));
        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));
        final Block lastBlock = Nxt.getBlockchain().getLastBlock();

        List<Record> actual = runTaxReport(height, BOB, ALICE, FORGY);
        assertEquals(
                asList(
                        mining().buy(1000000, FXT).commentAndGroup(lastBlock),
                        mining().buy(1, AEUR).fee(1000000, FXT).commentAndGroup(generateBlockId),
                        spend().sell(1000, AEUR).fee(1, AEUR).commentAndGroup(sendMoneyId),
                        income().buy(1000, AEUR).commentAndGroup(sendMoneyId)),
                actual);
    }

    @Test
    public void blockFeeReceived() {
        generateBlock();
        int height = getHeight();

        sendAeur(BOB, ALICE, 1000);

        final Transaction generateBlockId = transactionsRule.findOnly(type(ChildBlockFxtTransactionType.INSTANCE));
        final Block lastBlock = Nxt.getBlockchain().getLastBlock();

        List<Record> actual = runTaxReport(height, FORGY);
        assertEquals(
                asList(
                        mining().buy(1000000, FXT).commentAndGroup(lastBlock),
                        mining().buy(1, AEUR).fee(1000000, FXT).commentAndGroup(generateBlockId)),
                actual);
    }

    @Test
    public void sendAsset() {
        long assetId = AssetExchangeTest.issueAsset(BOB, ASSET_NAME).getAssetId();
        generateBlock();

        int height = getHeight();

        sendAsset(assetId, BOB, ALICE, 1000);
        generateBlock();

        final Transaction sendAssetId = transactionsRule.findOnly(type(ASSET_TRANSFER));

        List<Record> actual = runTaxReport(height, BOB);
        assertEquals(
                singletonList(spend().sell(1000, assetDisplayName(ASSET_NAME), ASSET_DECIMALS).fee(IGNIS.ONE_COIN, IGNIS).commentAndGroup(sendAssetId)),
                actual);
    }

    private void sendAsset(long assetId, Tester sender, Tester recipient, int volume) {
        new TransferAssetBuilder(assetId, sender, recipient)
                .setQuantityQNT(volume)
                .setFee(IGNIS.ONE_COIN)
                .transfer();
    }

    @Test
    public void receiveMoney() {
        generateBlock();
        int height = getHeight();

        sendAeur(BOB, ALICE, 1000);
        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));

        generateBlock();

        List<Record> actual = runTaxReport(height, ALICE);
        assertEquals(
                singletonList(income().buy(1000, AEUR).commentAndGroup(sendMoneyId)),
                actual);
    }

    @Test
    public void noTransactions() {
        generateBlock();
        int height = getHeight();

        generateBlock();

        List<Record> actual = runTaxReport(height, BOB);
        assertEquals(emptyList(), actual);
    }

    @Test
    public void ignisDividendReceived() {
        int height = getHeight();

        new AssetExchangeTest().ignisDividend();
        final Transaction transferToBobId = transactionsRule.findOnly(type(ASSET_TRANSFER), recipient(BOB)); // 300
        final Transaction dividendPayId = transactionsRule.findOnly(type(DIVIDEND_PAYMENT));

        List<Record> actual = runTaxReport(height, BOB);

        assertEquals(
                asList(
                        income().buy(300 * 10000, assetDisplayName("divSender"), decimalsDivSender).commentAndGroup(transferToBobId), //pre dividend payment currency transfer
                        dividendIncome().buy(3 * IGNIS.ONE_COIN, IGNIS).commentAndGroup(dividendPayId)),
                actual);
    }

    private String assetDisplayName(String assetName) {
        final Transaction assetIssuance = transactionsRule.findLast(type(ASSET_ISSUANCE), assetName(assetName));
        return "asset-" + assetName + "-" + Long.toUnsignedString(assetIssuance.getId());
    }

    @Test
    public void issueCurrencyTest() {
        int height = getHeight();

        new TestCurrencyIssuance().issueCurrencyImpl();
        generateBlock();

        final Transaction currencyIssuanceId = transactionsRule.findOnly(type(CURRENCY_ISSUANCE));


        List<Record> actual = runTaxReport(height, ALICE);

        assertEquals(
                singletonList(issueCurrency().buy(initialSupplyQNT, currencyDisplayName, decimalsTest1).fee(40 * IGNIS.ONE_COIN, IGNIS).commentAndGroup(currencyIssuanceId)),
                actual);
    }

    @Test
    public void currencyDividendReceived() {
        int height = getHeight();

        new AssetExchangeTest().currencyDividend();
        final Transaction dividendPayId = transactionsRule.findOnly(type(DIVIDEND_PAYMENT));
        final Transaction transferToBobId = transactionsRule.findOnly(type(ASSET_TRANSFER), recipient(BOB));

        List<Record> actual = runTaxReport(height, BOB);

        assertEquals(
                asList(
                        income().buy(5555555, assetDisplayName("divSender"), decimalsDivSender).commentAndGroup(transferToBobId), //pre dividend payment currency transfer
                        dividendIncome().buy(555, currencyDisplayName, decimalsTest1).commentAndGroup(dividendPayId)),
                actual);
    }

    @Test
    public void ignisDividendPayed() {
        int height = getHeight();

        new AssetExchangeTest().ignisDividend();

        final Transaction assetIssuanceId = transactionsRule.findOnly(type(ASSET_ISSUANCE)); // 300
        final Transaction transferToBobId = transactionsRule.findOnly(type(ASSET_TRANSFER), recipient(BOB)); // 300
        final Transaction transferToChuckId = transactionsRule.findOnly(type(ASSET_TRANSFER), recipient(CHUCK)); // 200
        final Transaction transferToDaveId = transactionsRule.findOnly(type(ASSET_TRANSFER), recipient(DAVE)); // 100
        final Transaction dividendPayId = transactionsRule.findOnly(type(DIVIDEND_PAYMENT));

        List<Record> actual = runTaxReport(height, ALICE);

        final String assetName = assetDisplayName("divSender");
        assertEquals(
                asList(
                        issueAsset().buy(BigDecimal.valueOf(ASSET_ISSUE_FEE_NQT, IssueAssetBuilder.ASSET_DECIMALS).longValue(), assetName, decimalsDivSender).fee(ASSET_ISSUE_FEE_NQT, IGNIS).commentAndGroup(assetIssuanceId), //pre dividend payment currency transfer
                        spend().sell(300 * 10000, assetName, decimalsDivSender).fee(IGNIS.ONE_COIN, IGNIS).commentAndGroup(transferToBobId), //pre dividend payment currency transfer
                        spend().sell(200 * 10000, assetName, decimalsDivSender).fee(IGNIS.ONE_COIN, IGNIS).commentAndGroup(transferToChuckId), //pre dividend payment currency transfer
                        spend().sell(100 * 10000, assetName, decimalsDivSender).fee(IGNIS.ONE_COIN, IGNIS).commentAndGroup(transferToDaveId), //pre dividend payment currency transfer
                        dividend().sell((3 + 2 + 1) * IGNIS.ONE_COIN, IGNIS).fee(IGNIS.ONE_COIN, IGNIS).commentAndGroup(dividendPayId)),
                actual);
    }

    @Test
    public void testAssetVoting() {
        final int decimalsTest1 = 4;
        final int decimalsTest2 = 4;

        int height = getHeight();

        new PhasingOnlyTest().testAssetVoting();
        generateBlocks(20);

        final Transaction assetIssuanceId = transactionsRule.findOnly(type(ASSET_ISSUANCE), assetName("TestAsset"));
        final Transaction assetIssuance2Id = transactionsRule.findOnly(type(ASSET_ISSUANCE), assetName("TestAsset2"));
        final Transaction sendMoneyId = transactionsRule.findOnly(type(PaymentTransactionType.ORDINARY));
        final Transaction voteCastingId = transactionsRule.findOnly(type(PHASING_VOTE_CASTING));

        List<Record> actual = runTaxReport(height, BOB);

        assertEquals(
                asList(
                        issueAsset().buy(10000, assetDisplayName("TestAsset"), decimalsTest1)
                                .fee(IGNIS.ONE_COIN * 1000, IGNIS).commentAndGroup(assetIssuanceId),
                        issueAsset().buy(10000, assetDisplayName("TestAsset2"), decimalsTest2)
                                .fee(IGNIS.ONE_COIN * 1000, IGNIS).commentAndGroup(assetIssuance2Id),
                        record("PHASING_VOTE_CASTING").sell(IGNIS.ONE_COIN, IGNIS).comment(voteCastingId).group(TRANSACTION_FEE),
                        income().buy(IGNIS.ONE_COIN, IGNIS).commentAndGroup(sendMoneyId)),
                actual);
    }

    @Test
    public void assetExchange() {
        int height = getHeight();

        new AssetExchangeTest().tradeAsset();

        final Transaction bidOrderId = transactionsRule.findOnly(type(BID_ORDER_PLACEMENT));

        List<Record> actual = runTaxReport(height, BOB);

        long sellVolume = unitRateToAmount(10, IssueAssetBuilder.ASSET_DECIMALS, 2 * IGNIS.ONE_COIN, IGNIS.getDecimals());

        assertEquals(
                singletonList(
                        trade().sell(sellVolume, IGNIS).buy(10, assetDisplayName("divSender"), decimalsDivSender)
                                .fee(IGNIS.ONE_COIN, IGNIS).comment(bidOrderId).group(ASSET_TRADE)),
                actual);
    }

    @Test
    public void assetExchangeWithOrderExchangeGap() {
        int height = getHeight();

        tradeAssetWithOrderExchangeGap();

        final Transaction bidOrderId = transactionsRule.findOnly(type(BID_ORDER_PLACEMENT));

        final List<Transaction> messages = transactionsRule.findAtLeastOne(type(ARBITRARY_MESSAGE), sender(BOB));

        long sellVolume = unitRateToAmount(10, IssueAssetBuilder.ASSET_DECIMALS, 2 * IGNIS.ONE_COIN, IGNIS.getDecimals());


        final List<Record> expectedBob = new ArrayList<>();

        expectedBob.add(record("ASSET_BID_ORDER_PLACEMENT").sell(IGNIS.ONE_COIN, IGNIS).comment(bidOrderId).group(TRANSACTION_FEE));

        expectedBob.addAll(messages.stream()
                .map(t -> record("ARBITRARY_MESSAGE").sell(1, AEUR).comment(t).group(TRANSACTION_FEE))
                .collect(Collectors.toList()));

        expectedBob.add(trade().sell(sellVolume, IGNIS).buy(10, assetDisplayName("divSender"), decimalsDivSender)
                .comment(bidOrderId).group(ASSET_TRADE));

        assertEquals(
                expectedBob,
                runTaxReport(height, BOB));
    }


    private void tradeAssetWithOrderExchangeGap() {
        String assetId = AssetExchangeTest.issueAsset(ALICE, "divSender").getAssetIdString();

        long fee = IGNIS.ONE_COIN;

        new PlaceAssetOrderBuilder(BOB, assetId, 10, IGNIS.ONE_COIN * 2)
                .setFeeNQT(fee)
                .placeBidOrder();

        generateBlock();

        for (int i = 0; i < 150; i++) {
            SendMessageCall.create(AEUR.getId())
                    .secretPhrase(BOB.getSecretPhrase())
                    .recipient(CHUCK.getId())
                    .message("Some message")
                    .feeNQT(1)
                    .build()
                    .invokeNoError();
            generateBlock();
        }

        new PlaceAssetOrderBuilder(ALICE, assetId, 10, IGNIS.ONE_COIN * 2)
                .setFeeNQT(fee)
                .placeAskOrder();

        generateBlock();
    }

    @Test
    public void coinExchange() {
        int height = getHeight();

        new CoinExchangeTest().simpleExchange();
        final Transaction orderIssueId = transactionsRule.findFirst(type(CoinExchangeTransactionType.ORDER_ISSUE));

        List<Record> actual = runTaxReport(height, ALICE);

        assertEquals(
                singletonList(
                        trade().sell(100 * IGNIS.ONE_COIN, IGNIS).buy(25 * AEUR.ONE_COIN, AEUR)
                                .fee(1000000, IGNIS).comment(orderIssueId).group(COIN_EXCHANGE_TRADE)),
                actual);
    }

    @Test
    public void coinMultiOrderExchange() {
        int height = getHeight();

        new CoinExchangeTest().multiOrderExchange();

        final Transaction orderIssueId = transactionsRule.findFirst(type(CoinExchangeTransactionType.ORDER_ISSUE));

        List<Record> actual = runTaxReport(height, ALICE);

        assertEquals(
                asList(
                        trade().sell(60 * IGNIS.ONE_COIN, IGNIS).buy(15 * AEUR.ONE_COIN, AEUR)
                                .fee(1000000, IGNIS).comment(orderIssueId).group(COIN_EXCHANGE_TRADE),
                        trade().sell(40 * IGNIS.ONE_COIN, IGNIS).buy(10 * AEUR.ONE_COIN, AEUR)
                                .commentAndGroup(orderIssueId).group(COIN_EXCHANGE_TRADE)),
                actual);
    }

    @Test
    public void currencyExchangeBuy() {
        int height = getHeight();

        new TestCurrencyExchange().buyCurrency();
        final Transaction currencyIssuanceId = transactionsRule.findOnly(type(CURRENCY_ISSUANCE));
        final Transaction exchangeOfferId = transactionsRule.findOnly(type(PUBLISH_EXCHANGE_OFFER));
        List<Record> actual = runTaxReport(height, ALICE);

        assertEquals(
                asList(
                        issueCurrency().buy(initialSupplyQNT, currencyDisplayName, decimalsTest1)
                                .fee(40 * IGNIS.ONE_COIN, IGNIS).commentAndGroup(currencyIssuanceId),
                        trade().sell(200, currencyDisplayName, decimalsTest1).buy(200 * 105, IGNIS)
                                .fee(IGNIS.ONE_COIN, IGNIS).comment(exchangeOfferId).group(CURRENCY_EXCHANGE)),
                actual);
    }

    @Test
    public void currencyExchangeSell() {
        int height = getHeight();


        new TestCurrencyExchange().sellCurrency();
        final Transaction currencyIssuanceId = transactionsRule.findOnly(type(CURRENCY_ISSUANCE));
        final Transaction sendMoneyId = transactionsRule.findOnly(type(CURRENCY_TRANSFER));
        final Transaction exchangeOfferId = transactionsRule.findOnly(type(PUBLISH_EXCHANGE_OFFER));

        List<Record> actual = runTaxReport(height, ALICE);

        assertEquals(
                asList(
                        issueCurrency().buy(initialSupplyQNT, currencyDisplayName, decimalsTest1)
                                .fee(40 * IGNIS.ONE_COIN, IGNIS).commentAndGroup(currencyIssuanceId),
                        trade().sell(200 * 95, IGNIS).buy(200, currencyDisplayName, decimalsTest1)
                                .fee(IGNIS.ONE_COIN, IGNIS).comment(exchangeOfferId).group(CURRENCY_EXCHANGE),
                        spend().sell(2000, currencyDisplayName, decimalsTest1).fee(IGNIS.ONE_COIN, IGNIS)
                                .commentAndGroup(sendMoneyId)),
                actual);
    }

    @Test
    public void testApprovePendingTransaction() {
        int height = getHeight();

        new PhasingOnlyTest().testRejectingPendingTransaction();
        generateBlock();

        final Transaction sendMoneyId = transactionsRule.findOnly(type(PaymentTransactionType.ORDINARY));

        List<Record> actual = runTaxReport(height, BOB);

        assertEquals(
                singletonList(income().buy(IGNIS.ONE_COIN, IGNIS).commentAndGroup(sendMoneyId)),
                actual);
    }

    @Test
    public void testRejectingPendingTransaction() {
        int height = getHeight();

        new PhasingOnlyTest().testRejectingPendingTransaction2();
        generateBlock();

        List<Record> actual = runTaxReport(height, BOB);

        assertEquals(emptyList(), actual);
    }

    static void sendAeur(Tester sender, Tester recipient, long amount) {
        SendMoneyCall.create(AEUR.getId())
                .secretPhrase(sender.getSecretPhrase())
                .recipient(recipient.getId())
                .amountNQT(amount)
                .feeNQT(1)
                .build()
                .invokeNoError();
        generateBlock();
    }

    static void sendMessage(Tester sender, Tester recipient) {
        SendMessageCall.create(AEUR.getId())
                .secretPhrase(sender.getSecretPhrase())
                .recipient(recipient.getId())
                .message("Some message")
                .feeNQT(1)
                .build()
                .invokeNoError();
        generateBlock();
    }

    List<Record> runTaxReport(int height, Tester... testers) {
        ListLineWriter lineWriter = new ListLineWriter();
        LongPredicate predicate = testers.length == 0
                ? id -> true
                : id -> Stream.of(testers).mapToLong(Tester::getId).anyMatch(testerId -> testerId == id);
        try (TaxReport tested = new TaxReport(predicate, h -> true, instant -> instant.atZone(ZoneId.of("UTC")).format(ISO_LOCAL_DATE), lineWriter, UnknownEventPolicy.FAIL)) {
            tested.init();
            rescan(height);
        }
        return lineWriter.getList().stream().map(Record::new).collect(toList());
    }

    private void rescan(int height) {
        ScanCall.create()
                .height(height)
                .validate(false)
                .adminPassword("")
                .build()
                .invokeNoError();
    }

    private static class ListLineWriter implements LineWriter {
        private final List<Map<Column, String>> list = new ArrayList<>();

        @Override
        public void close() {
        }

        @Override
        public void writeLine(Map<Column, String> line) {
            Map<Column, String> record = new EnumMap<>(Column.class);
            record.putAll(line);
            list.add(record);
        }

        List<Map<Column, String>> getList() {
            return list;
        }
    }
}