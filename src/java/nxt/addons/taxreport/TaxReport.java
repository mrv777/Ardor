package nxt.addons.taxreport;

import nxt.account.AccountLedger;
import nxt.account.AccountLedger.LedgerEntry;
import nxt.account.AccountLedger.LedgerEvent;
import nxt.account.AccountLedger.LedgerHolding;
import nxt.ae.Asset;
import nxt.blockchain.Chain;
import nxt.blockchain.Transaction;
import nxt.blockchain.TransactionHome;
import nxt.ms.Currency;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;

import java.io.Closeable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static nxt.account.AccountLedger.LedgerEvent.TRANSACTION_FEE;
import static nxt.addons.taxreport.Column.BUY_CURRENCY;
import static nxt.addons.taxreport.Column.BUY_VOLUME;
import static nxt.addons.taxreport.Column.COMMENT;
import static nxt.addons.taxreport.Column.DATE;
import static nxt.addons.taxreport.Column.FEE_CURRENCY;
import static nxt.addons.taxreport.Column.FEE_VOLUME;
import static nxt.addons.taxreport.Column.GROUP;
import static nxt.addons.taxreport.Column.SELL_CURRENCY;
import static nxt.addons.taxreport.Column.SELL_VOLUME;
import static nxt.addons.taxreport.Column.TYPE;

public class TaxReport implements Closeable {
    private final LineWriter lineWriter;
    private final Predicate<LedgerEntry> ledgerEntryPredicate;
    private final LimitedGrouper<EventIdAdnAccountId, LedgerEntry> ledgerEntries = new LimitedGrouper<>();
    private final Function<Instant, String> timestampFormatter;
    private final Listener<LedgerEntry> ledgerEventListener = this::onLedgerEvent;
    private final Function<List<LedgerEntry>, List<Record>> unknownEventPolicy;

    public TaxReport(LongPredicate accounts, IntPredicate heightsPredicate, Function<Instant, String> timestampFormatter, LineWriter lineWriter, UnknownEventPolicy unknownEventPolicy) {
        this.timestampFormatter = timestampFormatter;
        this.unknownEventPolicy = unknownEventPolicy;
        this.ledgerEntryPredicate = event -> accounts.test(event.getAccountId()) && heightsPredicate.test(event.getHeight());
        this.lineWriter = lineWriter;
    }

    public void init() {
        AccountLedger.addListener(ledgerEventListener, AccountLedger.Event.ADD_ENTRY);
    }

    private void onLedgerEvent(LedgerEntry ledgerEntry) {
        if (!ledgerEntryPredicate.test(ledgerEntry)) {
            return;
        }

        Map.Entry<EventIdAdnAccountId, List<LedgerEntry>> oldest = ledgerEntries.offer(new EventIdAdnAccountId(ledgerEntry), ledgerEntry);
        if (oldest == null) {
            return;
        }
        onGroupedLedgerEvents(oldest.getValue());
    }

    private Optional<Function<List<LedgerEntry>, List<Record>>> getFunction(List<LedgerEntry> entries) {
        final Optional<Function<List<LedgerEntry>, List<Record>>> result = entries.stream()
                .map(ledgerEntry -> {
                    switch (ledgerEntry.getEvent()) {
                        case REJECT_PHASED_TRANSACTION:
                        case ACCOUNT_INFO:
                        case ALIAS_ASSIGNMENT:
                        case ALIAS_BUY:
                        case ALIAS_DELETE:
                        case ALIAS_SELL:
                        case ARBITRARY_MESSAGE:
                        case PHASING_VOTE_CASTING:
                        case POLL_CREATION:
                        case VOTE_CASTING:
                        case ACCOUNT_PROPERTY_SET:
                        case ACCOUNT_PROPERTY_DELETE:
                        case ASSET_ASK_ORDER_CANCELLATION:
                        case ASSET_ASK_ORDER_PLACEMENT:
                        case ASSET_BID_ORDER_CANCELLATION:
                        case ASSET_BID_ORDER_PLACEMENT:
                        case ASSET_INCREASE:
                        case ASSET_SET_PHASING_CONTROL:
                        case ASSET_PROPERTY_SET:
                        case ASSET_PROPERTY_DELETE:
                        case DIGITAL_GOODS_DELISTED:
                        case DIGITAL_GOODS_DELISTING:
                        case DIGITAL_GOODS_DELIVERY:
                        case DIGITAL_GOODS_FEEDBACK:
                        case DIGITAL_GOODS_LISTING:
                        case DIGITAL_GOODS_PRICE_CHANGE:
                        case DIGITAL_GOODS_PURCHASE:
                        case DIGITAL_GOODS_PURCHASE_EXPIRED:
                        case DIGITAL_GOODS_QUANTITY_CHANGE:
                        case DIGITAL_GOODS_REFUND:
                        case ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        case ACCOUNT_CONTROL_PHASING_ONLY:
                        case CURRENCY_DISTRIBUTION:
                        case CURRENCY_EXCHANGE_BUY:
                        case CURRENCY_EXCHANGE_SELL:
                        case CURRENCY_OFFER_EXPIRED:
                        case CURRENCY_OFFER_REPLACED:
                        case CURRENCY_PUBLISH_EXCHANGE_OFFER:
                        case CURRENCY_RESERVE_CLAIM:
                        case CURRENCY_RESERVE_INCREASE:
                        case CURRENCY_UNDO_CROWDFUNDING:
                        case TAGGED_DATA_UPLOAD:
                        case SHUFFLING_REGISTRATION:
                        case SHUFFLING_PROCESSING:
                        case SHUFFLING_CANCELLATION:
                        case SHUFFLING_DISTRIBUTION:
                        case COIN_EXCHANGE_ORDER_ISSUE:
                        case COIN_EXCHANGE_ORDER_CANCEL:
                        case CONTRACT_REFERENCE_SET:
                        case CONTRACT_REFERENCE_DELETE:
                        case ADD_PERMISSION:
                        case REMOVE_PERMISSION:

                        case FXT_PAYMENT:
                        case ASSET_DIVIDEND_PAYMENT:
                        case ASSET_TRANSFER:
                        case ORDINARY_PAYMENT:
                        case ASSET_ISSUANCE:
                        case CURRENCY_ISSUANCE:
                        case CURRENCY_TRANSFER:
                        case BLOCK_GENERATED:
                        case CHILD_BLOCK:
                        case CURRENCY_DELETION:
                        case ASSET_DELETE:
                        case CURRENCY_MINTING:
                            return SingleEventOperation.INSTANCE;
                        case COIN_EXCHANGE_TRADE:
                        case CURRENCY_EXCHANGE:
                        case ASSET_TRADE:
                            return ExchangeOperation.INSTANCE;
                        case TRANSACTION_FEE:
                            return null;
                        default:
                            return unknownEventPolicy;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
        if (result.isPresent()) {
            return result;
        }
        return Optional.of(FeesOnlyOperation.INSTANCE);

    }

    private static Holding getHolding(LedgerHolding holding, long holdingId) {
        switch (holding) {
            case UNCONFIRMED_COIN_BALANCE:
            case COIN_BALANCE:
                return new Holding(Chain.getChain((int) holdingId));
            case UNCONFIRMED_ASSET_BALANCE:
            case ASSET_BALANCE:
                return new Holding(Asset.getAsset(holdingId));
            case UNCONFIRMED_CURRENCY_BALANCE:
            case CURRENCY_BALANCE:
                return new Holding(Currency.getCurrency(holdingId));
            default:
                throw new IllegalArgumentException("Unsupported ledger holding: " + holding);
        }
    }

    private static class Holding {
        private final String name;
        private final int decimals;

        private Holding(String name, int decimals) {
            this.name = name;
            this.decimals = decimals;
        }

        public Holding(Chain chain) {
            this(chain.getName(), chain.getDecimals());
        }

        public Holding(Asset asset) {
            this("asset-" + asset.getName() + "-" + Long.toUnsignedString(asset.getId()), asset.getDecimals());
        }

        public Holding(Currency currency) {
            this(currency.getCode() + "@" + currency.getChildChain().getName(), currency.getDecimals());
        }

        public String getName() {
            return name;
        }

        public String formatValue(long value) {
            return String.format("%s", BigDecimal.valueOf(value, decimals));
        }
    }

    private String getDateString(int timestamp) {
        long millis = Convert.fromEpochTime(timestamp);
        return timestampFormatter.apply(Instant.ofEpochMilli(millis));
    }

    @Override
    public void close() {
        AccountLedger.removeListener(ledgerEventListener, AccountLedger.Event.ADD_ENTRY);
        flush();
        lineWriter.close();
    }

    private void flush() {
        ledgerEntries.forEach((k, v) -> this.onGroupedLedgerEvents(v));

        ledgerEntries.clear();
    }

    private void onGroupedLedgerEvents(List<LedgerEntry> ledgerEntries) {
        try {
            getFunction(ledgerEntries)
                    .map(f -> f.apply(ledgerEntries))
                    .ifPresent(lines -> {
                        lines.forEach(line -> {
                            line.date(getDateString(ledgerEntries.stream().mapToInt(LedgerEntry::getTimestamp).min().getAsInt()));
                            lineWriter.writeLine(line.getLine());
                        });
                    });
        } catch (Throwable t) {
            Logger.logErrorMessage("Problem running tax report plugin: ", t);
            lineWriter.writeLine(Collections.singletonMap(COMMENT, "Problem running tax report plugin: " + t.getMessage()));
        }
    }

    private enum SingleEventOperation implements Function<List<LedgerEntry>, List<Record>> {
        INSTANCE;

        @Override
        public List<Record> apply(List<LedgerEntry> ledgerEntries) {
            if (ledgerEntries.isEmpty() || ledgerEntries.size() > 2) {
                throw getUnexpectedEntriesException(ledgerEntries);
            }
            return singletonList(createLine(findEntry(ledgerEntries), findFee(ledgerEntries)));
        }

        private LedgerEntry findEntry(List<LedgerEntry> entries) {
            return entries.stream()
                    .filter(entry -> entry.getEvent() != TRANSACTION_FEE)
                    .findFirst()
                    .orElseThrow(() -> getUnexpectedEntriesException(entries));
        }

        private Record createLine(LedgerEntry ledgerEntry, Optional<LedgerEntry> fee) {
            Record record = new Record();
            long quantityQNT = ledgerEntry.getChange();
            final Holding holding = getHolding(ledgerEntry.getHolding(), ledgerEntry.getHoldingId());

            if (quantityQNT >= 0) {
                record.type(getIncomeType(ledgerEntry.getEvent()));
                record.buy(holding, quantityQNT);
            } else {
                record.type(getSpendType(ledgerEntry.getEvent()));
                record.sell(holding, quantityQNT);
            }
            record.comment(ledgerEntry.getEventId(), ledgerEntry.getEvent());
            fee.ifPresent(f -> record.fee(f.getChainId(), f.getChange()));

            return record;
        }

        private static String getSpendType(LedgerEvent event) {
            switch (event) {
                case ASSET_TRANSFER:
                case CURRENCY_TRANSFER:
                case ORDINARY_PAYMENT:
                case FXT_PAYMENT:
                    return "Spend";
                default:
                    return event.name();
            }
        }

        private static String getIncomeType(LedgerEvent event) {
            switch (event) {
                case ASSET_DIVIDEND_PAYMENT:
                    return "Dividends Income";
                case ASSET_TRANSFER:
                case ORDINARY_PAYMENT:
                case FXT_PAYMENT:
                    return "Income";
                case BLOCK_GENERATED:
                case CHILD_BLOCK:
                case CURRENCY_MINTING:
                    return "Mining";
                default:
                    return event.name();
            }
        }
    }

    public enum UnknownEventPolicy implements Function<List<LedgerEntry>, List<Record>> {
        IGNORE {
            @Override
            public List<Record> apply(List<LedgerEntry> ledgerEntries) {
                Logger.logWarningMessage("Ignored unknown ledger entries: " + ledgerEntriesToString(ledgerEntries));
                return Collections.emptyList();
            }

        },
        FAIL {
            @Override
            public List<Record> apply(List<LedgerEntry> ledgerEntries) {
                throw getUnexpectedEntriesException(ledgerEntries);
            }
        }
    }

    private enum FeesOnlyOperation implements Function<List<LedgerEntry>, List<Record>> {
        INSTANCE;

        @Override
        public List<Record> apply(List<LedgerEntry> ledgerEntries) {
            if (ledgerEntries.isEmpty() || ledgerEntries.size() > 2) {
                throw getUnexpectedEntriesException(ledgerEntries);
            }
            return createLines(findFees(ledgerEntries));
        }

        private static List<LedgerEntry> findFees(List<LedgerEntry> entries) {
            return entries.stream().filter(entry -> entry.getEvent() == TRANSACTION_FEE).collect(toList());
        }

        private List<Record> createLines(List<LedgerEntry> fees) {
            return fees.stream()
                    .collect(Collectors.groupingBy(TmpHolding::new))
                    .entrySet().stream()
                    .flatMap(entry -> {
                        final TmpHolding tmpHolding = entry.getKey();
                        final Holding holding = getHolding(tmpHolding.holding, tmpHolding.holdingId);

                        return entry.getValue().stream().map(f -> {
                            Record record = new Record();

                            record.type(getEventType(f).name());
                            record.sell(holding, f.getChange());
                            record.comment(f.getEventId(), f.getEvent());

                            return record;

                        });
                    }).collect(toList());
        }

        private LedgerEvent getEventType(LedgerEntry ledgerEntry) {
            final TransactionHome transactionHome = Chain.getChain(ledgerEntry.getChainId()).getTransactionHome();
            Transaction transaction = transactionHome.findTransaction(ledgerEntry.getEventHash());
            return transaction.getType().getLedgerEvent();
        }

        private static class TmpHolding {
            private final long holdingId;
            private final LedgerHolding holding;

            private TmpHolding(LedgerEntry entry) {
                this(entry.getHoldingId(), entry.getHolding());
            }

            private TmpHolding(long holdingId, LedgerHolding holding) {
                this.holdingId = holdingId;
                this.holding = holding;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TmpHolding that = (TmpHolding) o;

                if (holdingId != that.holdingId) return false;
                return holding == that.holding;
            }

            @Override
            public int hashCode() {
                int result = (int) (holdingId ^ (holdingId >>> 32));
                result = 31 * result + holding.hashCode();
                return result;
            }
        }
    }

    private static Optional<LedgerEntry> findEntry(List<LedgerEntry> entries, Predicate<LedgerEntry> predicate) {
        return entries.stream().filter(predicate).findFirst();
    }

    private static Optional<LedgerEntry> findFee(List<LedgerEntry> entries) {
        return findEntry(entries, entry -> entry.getEvent() == TRANSACTION_FEE);
    }

    private enum ExchangeOperation implements Function<List<LedgerEntry>, List<Record>> {
        INSTANCE;

        private enum EntryType {
            FEE, BUY, SELL
        }

        private static Map<EntryType, List<LedgerEntry>> groupByEntryType(List<LedgerEntry> ledgerEntries) {
            return ledgerEntries.stream().collect(groupingBy(entry -> {
                if (entry.getEvent() == TRANSACTION_FEE) {
                    return EntryType.FEE;
                }
                if (entry.getChange() > 0) {
                    return EntryType.BUY;
                }
                return EntryType.SELL;
            }));
        }

        @Override
        public List<Record> apply(List<LedgerEntry> ledgerEntries) {
            Map<EntryType, List<LedgerEntry>> map = groupByEntryType(ledgerEntries);
            List<LedgerEntry> buys = map.getOrDefault(EntryType.BUY, emptyList());
            List<LedgerEntry> sells = map.getOrDefault(EntryType.SELL, emptyList());
            if (buys.isEmpty() || buys.size() != sells.size()) {
                throw getUnexpectedEntriesException(ledgerEntries);
            }
            Iterator<LedgerEntry> buysIt = buys.iterator();
            Iterator<LedgerEntry> sellsIt = sells.iterator();
            List<Record> result = new ArrayList<>();
            result.add(createLine(sellsIt.next(), buysIt.next(), map.getOrDefault(EntryType.FEE, emptyList()).stream().findFirst()));
            while (buysIt.hasNext()) {
                result.add(createLine(sellsIt.next(), buysIt.next(), Optional.empty()));
            }
            return result;
        }

        private Record createLine(LedgerEntry sellEntry, LedgerEntry buyEntry, Optional<LedgerEntry> fee) {
            Record record = new Record();

            record.type("Trade");
            Holding buyHolding = getHolding(buyEntry.getHolding(), buyEntry.getHoldingId());
            record.buy(buyHolding, buyEntry.getChange());

            Holding sellHolding = getHolding(sellEntry.getHolding(), sellEntry.getHoldingId());
            record.sell(sellHolding, sellEntry.getChange());

            fee.ifPresent(f -> record.fee(f.getChainId(), f.getChange()));
            record.comment(buyEntry.getEventId(), buyEntry.getEvent());

            return record;
        }
    }

    private static RuntimeException getUnexpectedEntriesException(List<LedgerEntry> ledgerEntries) {
        return new RuntimeException("Unexpected ledger entries: " + ledgerEntriesToString(ledgerEntries));
    }

    private static String ledgerEntriesToString(List<LedgerEntry> ledgerEntries) {
        return ledgerEntries.stream()
                .map(TaxReport::ledgerEntryToString)
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String ledgerEntryToString(LedgerEntry ledgerEntry) {
        return new StringJoiner(", ", LedgerEntry.class.getSimpleName() + "[", "]")
                .add("ledgerId=" + ledgerEntry.getLedgerId())
                .add("event=" + ledgerEntry.getEvent())
                .add("eventId=" + ledgerEntry.getEventId())
                .add("chainId=" + ledgerEntry.getChainId())
                .add("accountId=" + ledgerEntry.getAccountId())
                .add("holding=" + ledgerEntry.getHolding())
                .add("holdingId=" + ledgerEntry.getHoldingId())
                .add("change=" + ledgerEntry.getChange())
                .add("balance=" + ledgerEntry.getBalance())
                .add("blockId=" + ledgerEntry.getBlockId())
                .add("height=" + ledgerEntry.getHeight())
                .add("timestamp=" + ledgerEntry.getTimestamp())
                .toString();
    }

    private static class EventIdAdnAccountId {
        private final long eventId;
        private final long accountId;

        public EventIdAdnAccountId(LedgerEntry entry) {
            eventId = entry.getEventId();
            accountId = entry.getAccountId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EventIdAdnAccountId that = (EventIdAdnAccountId) o;

            if (eventId != that.eventId) return false;
            return accountId == that.accountId;
        }

        @Override
        public int hashCode() {
            int result = (int) (eventId ^ (eventId >>> 32));
            return 31 * result + (int) (accountId ^ (accountId >>> 32));
        }
    }

    private static class Record {
        private final Map<Column, String> line = new EnumMap<>(Column.class);

        void type(String type) {
            line.put(TYPE, type);
        }

        void date(String date) {
            line.put(DATE, date);
        }

        void buy(Holding holding, long change) {
            line.put(BUY_VOLUME, holding.formatValue(change));
            line.put(BUY_CURRENCY, holding.getName());
        }

        void sell(Holding holding, long change) {
            line.put(SELL_VOLUME, holding.formatValue(-change));
            line.put(SELL_CURRENCY, holding.getName());
        }

        void fee(long chainId, long change) {
            final Holding holding = new Holding(Chain.getChain((int) chainId));
            fee(holding, change);
        }

        void fee(Holding holding, long change) {
            line.put(FEE_VOLUME, holding.formatValue(Math.abs(change)));
            line.put(FEE_CURRENCY, holding.getName());
        }

        Map<Column, String> getLine() {
            return line;
        }

        public void comment(long transactionId, LedgerEvent event) {
            final String prefix = event.isTransaction() ? "Transaction id: " : "Block id: ";
            line.put(COMMENT, prefix + Long.toUnsignedString(transactionId));
            line.put(GROUP, event.name());
        }
    }
}