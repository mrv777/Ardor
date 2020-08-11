package nxt.addons;

import nxt.Nxt;
import nxt.NxtException;
import nxt.account.Account;
import nxt.addons.taxreport.PrintWriterLineWriter;
import nxt.addons.taxreport.TaxReport;
import nxt.addons.taxreport.TaxReport.UnknownEventPolicy;
import nxt.blockchain.Block;
import nxt.blockchain.Blockchain;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.http.APITag;
import nxt.http.JSONResponses;
import nxt.http.ParameterException;
import nxt.http.ParameterParser;
import nxt.http.callers.ScanCall;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

public class TaxReportAddOn implements AddOn {

    @Override
    public Map<String, APIRequestHandler> getAPIRequests() {
        return Collections.singletonMap("taxReport", getAPIRequestHandler1());
    }

    private APIRequestHandler getAPIRequestHandler1() {
        return new APIRequestHandler(new APITag[]{APITag.ADDONS}, "fromHeight", "toHeight", "account", "account", "fromDate", "toDate", "timeZone", "dateFormat", "delimiter", "unknownEventPolicy") {
            @Override
            protected JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws NxtException {
                ZoneId zoneId = getZoneId(request);
                int fromHeight = getFromHeight(request, zoneId);
                int toHeight = getToHeight(request, zoneId);
                DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(request);
                Function<Instant, String> timestampFormatter = getTimestampFormatter(dateTimeFormatter, zoneId);

                LongPredicate accountsPredicate = accountsToPredicate(ParameterParser.getAccounts(request));
                IntPredicate heightsPredicate = height -> height >= fromHeight && height <= toHeight;
                UnknownEventPolicy unknownEventPolicy = getUnknownEventPolicy(request);

                final String delimiter = getDelimiter(request);
                try (ServletOutputStream os = response.getOutputStream();
                     PrintWriterLineWriter lw = new PrintWriterLineWriter(new PrintWriter(os), "\"", delimiter);
                     TaxReport taxReport = new TaxReport(accountsPredicate, heightsPredicate, timestampFormatter, lw, unknownEventPolicy)) {

                    taxReport.init();
                    rescan(request.getParameter("adminPassword"), fromHeight);
                } catch (IOException e) {
                    return JSONResponses.RESPONSE_WRITE_ERROR;
                }
                return null;
            }

            private UnknownEventPolicy getUnknownEventPolicy(HttpServletRequest request) {
                String paramValue = Convert.emptyToNull(request.getParameter("unknownEventPolicy"));
                if (paramValue == null) {
                    return UnknownEventPolicy.FAIL;
                }
                try {
                    return UnknownEventPolicy.valueOf(paramValue);
                } catch (IllegalArgumentException e) {
                    return UnknownEventPolicy.FAIL;
                }
            }

            private String getDelimiter(HttpServletRequest request) {
                try {
                    return ParameterParser.getParameter(request, "delimiter");
                } catch (ParameterException e) {
                    return "\t";
                }
            }

            private DateTimeFormatter getDateTimeFormatter(HttpServletRequest request) {
                try {
                    return DateTimeFormatter.ofPattern(ParameterParser.getParameter(request, "dateFormat"));
                } catch (ParameterException e) {
                    return ISO_LOCAL_DATE;
                }
            }

            private Function<Instant, String> getTimestampFormatter(DateTimeFormatter dateTimeFormatter, ZoneId zoneId) {
                return date -> getDateString(date, dateTimeFormatter, zoneId);
            }

            private String getDateString(Instant instant, DateTimeFormatter dateTimeFormatter, ZoneId zoneId) {
                return instant.atZone(zoneId).format(dateTimeFormatter);
            }

            private ZoneId getZoneId(HttpServletRequest request) {
                try {
                    return ZoneId.of(ParameterParser.getParameter(request, "timeZone"));
                } catch (ParameterException e) {
                    return ZoneId.of("UTC");
                }
            }

            private int getToHeight(HttpServletRequest request, ZoneId zoneId) {
                try {
                    return getHeight(request, "toDate", "toHeight", zoneId);
                } catch (ParameterException e) {
                    return Integer.MAX_VALUE;
                }
            }

            private int getFromHeight(HttpServletRequest request, ZoneId zoneId) throws ParameterException {
                return getHeight(request, "fromDate", "fromHeight", zoneId);
            }

            private int getHeight(HttpServletRequest request, String dateParameter, String heightParameter, ZoneId zoneId) throws ParameterException {
                ZonedDateTime fromDate = getDate(request, dateParameter, zoneId);
                return fromDate != null
                        ? dateToHeight(fromDate)
                        : ParameterParser.getInt(request, heightParameter, 0, Integer.MAX_VALUE, true);
            }

            private ZonedDateTime getDate(HttpServletRequest request, String parameterName, ZoneId zoneId) {
                try {
                    String stringDate = ParameterParser.getParameter(request, parameterName);
                    return LocalDate.parse(stringDate, BASIC_ISO_DATE)
                            .atStartOfDay(zoneId);
                } catch (ParameterException e) {
                    return null;
                }
            }

            private void rescan(String adminPassword, int height) {
                ScanCall.create()
                        .height(height)
                        .validate(false)
                        .adminPassword(adminPassword)
                        .build()
                        .invokeNoError();
            }

            private int dateToHeight(ZonedDateTime date) {
                int timestamp = Convert.toEpochTime(date.toInstant().toEpochMilli());
                Blockchain blockchain = Nxt.getBlockchain();
                Block block = blockchain.getLastBlock(timestamp - 1);
                return block.getHeight();
            }


            @Override
            protected JSONStreamAware processRequest(HttpServletRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected boolean requirePassword() {
                return true;
            }

            @Override
            protected boolean requireFullClient() {
                return true;
            }

            @Override
            protected boolean allowRequiredBlockParameters() {
                return false;
            }

            @Override
            protected boolean isChainSpecific() {
                return false;
            }
        };
    }

    @Override
    public void init() {
        checkProperties();
    }

    private void checkProperties() {
        int ledgerTrimKeep = Nxt.getIntProperty("nxt.ledgerTrimKeep", -1);
        if (0 != ledgerTrimKeep) {
            throw new IllegalArgumentException("nxt.ledgerTrimKeep must be '0' to force ledger events logging. Actual value: " + ledgerTrimKeep);
        }

        if (0 != Nxt.getIntProperty("nxt.ledgerLogUnconfirmed", -1)) {
            throw new IllegalArgumentException("nxt.ledgerLogUnconfirmed must be '0' to force ledger events logging.");
        }
    }

    private static LongPredicate accountsToPredicate(List<Account> accounts) {
        long[] ids = accounts.stream().mapToLong(Account::getId).toArray();
        Arrays.sort(ids);
        return id -> Arrays.binarySearch(ids, id) >= 0;
    }
}
