package nxt.addons.taxreport;

import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.Tester;
import nxt.account.PaymentTransactionType;
import nxt.blockchain.Transaction;
import nxt.http.APICall;
import nxt.http.callers.TaxReportCall;
import nxt.util.Convert;
import nxt.util.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static nxt.addons.taxreport.Column.DATE;
import static nxt.addons.taxreport.Record.spend;
import static nxt.addons.taxreport.TransactionsRule.recipient;
import static nxt.addons.taxreport.TransactionsRule.sender;
import static nxt.addons.taxreport.TransactionsRule.type;
import static nxt.blockchain.ChildChain.AEUR;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

// requires nxt.addons.TaxReportAddOn
public class TaxReportAddOnTest extends TaxReportTest {
    private int originalNxtTime;

    static {
        BlockchainTest.putAdditionalProperty("nxt.disableSecurityPolicy", "true");
        BlockchainTest.putAdditionalProperty("nxt.ledgerTrimKeep", "0");
        BlockchainTest.putAdditionalProperty("nxt.ledgerLogUnconfirmed", "0");
    }

    @Before
    public void setUpTaxReportAddOnTest() {
        originalNxtTime = Nxt.getEpochTime();
    }

    @After
    public void tearDownTaxReportAddOnTest() {
        Nxt.setTime(new Time.CounterTime(originalNxtTime));
    }

    @Test
    public void testDates() {
        setNxtTime(daysFromToday(1));
        sendAeur(BOB, ALICE, 1000);
        generateBlock();

        setNxtTime(daysFromToday(3));
        sendAeur(BOB, ALICE, 3000);
        generateBlock();

        setNxtTime(daysFromToday(4));
        generateBlock();
        setNxtTime(daysFromToday(5));
        generateBlock();

        final Transaction sendMoneyIdFirst = transactionsRule.findFirst(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));
        final Transaction sendMoneyIdLast = transactionsRule.findLast(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));


        assertEquals(
                singletonList(spend().sell(3000, AEUR).fee(1, AEUR).dateIsoLocal(daysFromToday(3)).commentAndGroup(sendMoneyIdLast)),
                runTaxReport(daysFromToday(2), daysFromToday(4), BOB));
        assertEquals(
                asList(
                        spend().sell(1000, AEUR).fee(1, AEUR).dateIsoLocal(daysFromToday(1)).commentAndGroup(sendMoneyIdFirst),
                        spend().sell(3000, AEUR).fee(1, AEUR).dateIsoLocal(daysFromToday(3)).commentAndGroup(sendMoneyIdLast)),
                runTaxReport(daysFromToday(1), daysFromToday(4), BOB));
        assertEquals(
                singletonList(spend().sell(1000, AEUR).fee(1, AEUR).dateIsoLocal(daysFromToday(1)).commentAndGroup(sendMoneyIdFirst)),
                runTaxReport(daysFromToday(1), daysFromToday(2), BOB));
    }

    @Test
    public void testDateFormat() {
        ZonedDateTime expectedDate = daysFromToday(2);
        setNxtTime(expectedDate);
        sendAeur(BOB, ALICE, 1000);
        generateBlock();

        String dateFormat = "d MMM uuuu";

        assertEquals(
                expectedDate.format(DateTimeFormatter.ofPattern(dateFormat)),
                runTaxReport(daysFromToday(1), daysFromToday(3), dateFormat, BOB).get(0).get(DATE));
    }

    @Test
    public void testTimezoneForDateFormatting() {
        int height = getHeight();
        setNxtTime(daysFromToday(1));
        sendAeur(BOB, ALICE, 1000);
        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));
        generateBlock();

        assertEquals(
                singletonList(spend().sell(1000, AEUR).fee(1, AEUR).dateIsoLocal(daysFromToday(0)).commentAndGroup(sendMoneyId)),
                runTaxReportInTimezone(height, "UTC-1", BOB));
    }

    @Test
    public void testTimezoneForDateParameterParsing() {
        setNxtTime(daysFromToday(2));
        sendAeur(BOB, ALICE, 1000);
        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));
        generateBlock();

        assertEquals(
                emptyList(),
                runTaxReportInTimezone(daysFromToday(2), daysFromToday(3), "UTC-1", BOB));
        assertEquals(
                singletonList(
                        spend().sell(1000, AEUR).fee(1, AEUR).dateIsoLocal(daysFromToday(1)).commentAndGroup(sendMoneyId)),
                runTaxReportInTimezone(daysFromToday(1), daysFromToday(2), "UTC-1", BOB));
    }

    @Test
    public void sendMoneyDifferentSeparator() {
        generateBlock();
        int height = getHeight();

        sendAeur(BOB, ALICE, 1000);
        final Transaction sendMoneyId = transactionsRule.findOnly(sender(BOB), recipient(ALICE), type(PaymentTransactionType.ORDINARY));

        generateBlock();

        List<String> response = getResponseAsStrings(TaxReportCall.create()
                .fromHeight(height)
                .account(BOB.getRsAccount())
                .delimiter(",")
                .build());
        List<Record> actual = parseDsv(response, ",");
        assertEquals(
                singletonList(spend().sell(1000, AEUR).fee(1, AEUR).commentAndGroup(sendMoneyId)),
                actual);
    }


    List<Record> runTaxReportInTimezone(ZonedDateTime from, ZonedDateTime to, String timezone, Tester... testers) {
        List<String> response = getResponseAsStrings(TaxReportCall.create()
                .fromDate(BASIC_ISO_DATE.format(from.toLocalDate()))
                .toDate(BASIC_ISO_DATE.format(to.toLocalDate()))
                .account(Arrays.stream(testers).map(Tester::getRsAccount).toArray(String[]::new))
                .timeZone(timezone)
                .build());
        return parseTsv(response);
    }

    List<Record> runTaxReportInTimezone(int height, String timezone, Tester... testers) {
        List<String> response = getResponseAsStrings(TaxReportCall.create()
                .fromHeight(height)
                .account(Arrays.stream(testers).map(Tester::getRsAccount).toArray(String[]::new))
                .timeZone(timezone)
                .build());
        return parseTsv(response);
    }

    private ZonedDateTime daysFromToday(int days) {
        return ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS).plusDays(days).plusSeconds(10);
    }

    private void setNxtTime(ZonedDateTime date) {
        long millis = date.toInstant().toEpochMilli();
        int epochTime = Convert.toEpochTime(millis);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Nxt.setTime(new Time.CounterTime(epochTime));
            return null;
        });
    }

    @Override
    List<Record> runTaxReport(int height, Tester... testers) {
        List<String> response = getResponseAsStrings(TaxReportCall.create()
                .fromHeight(height)
                .account(Arrays.stream(testers).map(Tester::getRsAccount).toArray(String[]::new))
                .build());
        return parseTsv(response);
    }

    List<Record> runTaxReport(ZonedDateTime from, ZonedDateTime to, Tester... testers) {
        List<String> response = getResponseAsStrings(TaxReportCall.create()
                .fromDate(BASIC_ISO_DATE.format(from.toLocalDate()))
                .toDate(BASIC_ISO_DATE.format(to.toLocalDate()))
                .account(Arrays.stream(testers).map(Tester::getRsAccount).toArray(String[]::new))
                .build());
        return parseTsv(response);
    }

    List<Record> runTaxReport(ZonedDateTime from, ZonedDateTime to, String dateFormat, Tester... testers) {
        List<String> response = getResponseAsStrings(TaxReportCall.create()
                .fromDate(BASIC_ISO_DATE.format(from.toLocalDate()))
                .toDate(BASIC_ISO_DATE.format(to.toLocalDate()))
                .dateFormat(dateFormat)
                .account(Arrays.stream(testers).map(Tester::getRsAccount).toArray(String[]::new))
                .build());
        return parseTsv(response);
    }

    private List<Record> parseTsv(List<String> response) {
        return parseDsv(response, "\t");
    }

    private List<Record> parseDsv(List<String> response, String delimiter) {
        List<String[]> streams = response.stream()
                .map(s -> s.split(delimiter))
                .collect(toList());
        List<Column> headers = parseHeaders(streams.get(0));
        return streams.stream().skip(1)
                .map(ss -> {
                    Map<Column, String> result = new EnumMap<>(Column.class);
                    for (int i = 0; i < ss.length; i++) {
                        result.put(headers.get(i), ss[i]);
                    }
                    return result;
                })
                .map(Record::new)
                .collect(toList());
    }

    private List<Column> parseHeaders(String[] headers) {
        assertArrayEquals("Actual headers array: " + Arrays.toString(headers),
                Stream.of(Column.values()).map(Column::getLabel).toArray(String[]::new),
                headers);

        return Arrays.asList(Column.values());
    }

    private static List<String> getResponseAsStrings(APICall apiCall) {
        try {
            InputStream inputStream = apiCall.getInputStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(toList());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}