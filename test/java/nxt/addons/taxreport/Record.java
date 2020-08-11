package nxt.addons.taxreport;

import nxt.Nxt;
import nxt.account.AccountLedger.LedgerEvent;
import nxt.blockchain.Block;
import nxt.blockchain.Chain;
import nxt.blockchain.Transaction;
import nxt.util.Convert;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.stream.Collectors.toMap;
import static nxt.account.AccountLedger.LedgerEvent.BLOCK_GENERATED;
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

class Record {
    private final Map<Column, String> result = emptyRecord();

    private Record(String type) {
        result.put(TYPE, type);
        result.put(DATE, expectedDateString());
    }

    public Record(Map<Column, String> map) {
        result.putAll(map);
    }

    static Record spend() {
        return new Record("Spend");
    }

    static Record record(String type) {
        return new Record(type);
    }

    static Record mining() {
        return new Record("Mining");
    }

    static Record trade() {
        return new Record("Trade");
    }

    static Record income() {
        return new Record("Income");
    }

    static Record dividendIncome() {
        return new Record("Dividends Income");
    }

    static Record dividend() {
        return new Record("ASSET_DIVIDEND_PAYMENT");
    }

    static Record issueCurrency() {
        return new Record("CURRENCY_ISSUANCE");
    }

    static Record issueAsset() {
        return new Record("ASSET_ISSUANCE");
    }

    static Record deposit() {
        return new Record("Deposit");
    }

    Record dateIsoLocal(ZonedDateTime date) {
        return date(date.toLocalDate(), ISO_LOCAL_DATE);
    }

    Record date(LocalDate date, DateTimeFormatter formatter) {
        return date(date.format(formatter));
    }

    Record date(String date) {
        result.put(DATE, date);
        return this;
    }

    Record buy(long amount, Chain chain) {
        return buy(amount, chain.getName(), chain.getDecimals());
    }

    Record buy(long amount, String currency, int decimals) {
        result.put(BUY_VOLUME, formatVolume(amount, decimals));
        result.put(BUY_CURRENCY, currency);
        return this;
    }

    Record sell(long amount, Chain currency) {
        return sell(amount, currency.getName(), currency.getDecimals());
    }

    Record sell(long amount, String currency, int decimals) {
        result.put(SELL_VOLUME, formatVolume(amount, decimals));
        result.put(SELL_CURRENCY, currency);
        return this;
    }

    Record fee(long feeVolume, Chain feeChain) {
        result.put(FEE_VOLUME, formatVolume(feeVolume, feeChain));
        result.put(FEE_CURRENCY, feeChain.getName());
        return this;
    }

    private static String formatVolume(long volume, int decimals) {
        return "" + BigDecimal.valueOf(volume, decimals);
    }

    private static String formatVolume(long volume, Chain chain) {
        return formatVolume(volume, chain.getDecimals());
    }

    private static Map<Column, String> emptyRecord() {
        return Stream.of((Column.values()))
                .collect(toMap(Function.identity(), c -> "", (v1, v2) -> v2, () -> new EnumMap<>(Column.class)));
    }

    private static String expectedDateString() {
        long millis = Convert.fromEpochTime(Nxt.getEpochTime());
        return Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).format(ISO_LOCAL_DATE);
    }

    String get(Column column) {
        return result.get(column);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        return result.equals(record.result);
    }

    @Override
    public int hashCode() {
        return result.hashCode();
    }

    @Override
    public String toString() {
        return result.toString();
    }

    Record commentAndGroup(Block block) {
        return comment("Block id: " + Long.toUnsignedString(block.getId())).group(BLOCK_GENERATED);
    }

    Record group(LedgerEvent group) {
        return group(group.name());
    }

    private Record group(String group) {
        result.put(GROUP, group);
        return this;
    }

    Record commentAndGroup(Transaction transaction) {
        return comment(transaction).group(transaction.getType().getLedgerEvent());
    }

    Record comment(Transaction transaction) {
        return comment("Transaction id: " + Long.toUnsignedString(transaction.getId()));
    }

    private Record comment(String comment) {
        result.put(COMMENT, comment);
        return this;
    }
}
