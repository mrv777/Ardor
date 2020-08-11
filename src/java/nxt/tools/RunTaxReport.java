package nxt.tools;

import nxt.Nxt;
import nxt.addons.TaxReportAddOn;
import nxt.configuration.Setup;
import nxt.http.callers.GetBlockCall;
import nxt.http.callers.TaxReportCall;
import nxt.http.responses.BlockResponse;
import nxt.util.Convert;
import nxt.util.security.BlockchainPermission;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.util.Collections.emptyList;

/**
 * <pre>{@code
 * This command line utility produces tax report in tab-separated format. Time zone of report will match local.
 * Usage: <account> <from-date> [to-date [output-file-name]] [-waitForDownloadComplete] [-ignoreIncompleteDownload]
 * 	 account     - account to run report for
 * 	 from-date     - date in yyyymmdd format, inclusive
 * 	 to-date     - date in yyyymmdd format, inclusive, defaults to today
 * 	 output-file-name     - defaults to report-for_account_from-date_to-date.csv
 * By default this tool exits with warning if blockchain last block timestamp is before to-date.
 * Following flags modify this behavior:
 * 	 -ignoreIncompleteDownload     - run report for locally available blockchain
 * 	 -waitForDownloadComplete     - run wait for enough blocks downloaded before running this report
 * Optional additional parameters
 *   --unknownEventPolicy=<FAIL|IGNORE> - what to do with ledger events unknown by this tool. 'FAIL' by default.
 *   --timeZone=<timezone id> - 'UTC' by default, see java.time.ZoneId.of(java.lang.String) for more info
 *   --dateFormat=<date and time format> - 'dd-MM-yyyy HH:mm:ss' by default, see java.time.format.DateTimeFormatter.ofPattern(java.lang.String) for more info
 *   --delimiter=<delimiter to separate values> - '\t' by default, no character escaping is done.
 * Usage examples:
 * ARDOR-4RU9-TNCT-F3MU-8952K 20190101 20200101 output.csv
 * ARDOR-4RU9-TNCT-F3MU-8952K 20190101 20200101     - will create file with name report-for_ARDOR-4RU9-TNCT-F3MU-8952K_20190101_20200101.csv
 * }</pre>
 */
public class RunTaxReport {
    private static final ZoneId zoneId = ZoneId.systemDefault();

    public static void main(String[] args) throws IOException, InterruptedException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new BlockchainPermission("tools"));
            sm.checkPermission(new BlockchainPermission("lifecycle"));
        }

        new ArgParser(args).parse((account, fromDate, toDate, outputFile, waitForDownloadComplete, ignoreIncompleteDownload, settings) -> {
            Properties properties = new Properties();
            properties.put("nxt.addOns", TaxReportAddOn.class.getName());
            properties.put("nxt.ledgerTrimKeep", "0");
            properties.put("nxt.ledgerLogUnconfirmed", "0");

            Nxt.init(Setup.FULL_NODE, properties);

            checkBlockchainState(toDate, waitForDownloadComplete, ignoreIncompleteDownload);

            final TaxReportCall taxReportCall = TaxReportCall.create()
                    .setParamValidation(false)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .account(account)
                    .timeZone(zoneId.getId())
                    .delimiter(",")
                    .dateFormat("dd-MM-yyyy HH:mm:ss") // default date format from import example file
                    .adminPassword(Nxt.getStringProperty("nxt.adminPassword", "", true));
            settings.forEach(taxReportCall::param);
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                copyStream(taxReportCall.build().getInputStream(), os);
            } finally {
                Nxt.shutdown();
            }
        });
    }

    private static void checkBlockchainState(String toDate, boolean waitForDownloadComplete, boolean ignoreIncompleteDownload) throws ExitException, InterruptedException {
        ZonedDateTime toDateDate = asDate(toDate);
        final ZonedDateTime now = ZonedDateTime.now(zoneId);
        if (toDateDate.isAfter(now)) {
            toDateDate = now;
        }
        do {
            final BlockResponse blockResponse = BlockResponse.create(GetBlockCall.create().build().invokeNoError());
            final ZonedDateTime lastBlockDate = asDate(blockResponse.getTimestamp());

            if (lastBlockDate.isBefore(toDateDate)) {
                final String message = String.format("Blockchain download is incomplete, last block date is: %s, required date is: %s", lastBlockDate, toDateDate);
                if (ignoreIncompleteDownload) {
                    System.out.println(message);
                    System.out.println("Running report");
                    return;
                }
                if (!waitForDownloadComplete) {
                    throw new ExitException(message, 1);
                }
                System.out.println(message);
                final int minutesToWait = 1;
                System.out.println("Waiting " + minutesToWait + " minute(s) to re-try");
                Thread.sleep(TimeUnit.MINUTES.toMillis(minutesToWait));
            } else {
                return;
            }
        } while (waitForDownloadComplete);

    }

    private static ZonedDateTime asDate(String stringDate) {
        return LocalDate.parse(stringDate, BASIC_ISO_DATE).atStartOfDay(zoneId);
    }

    private static ZonedDateTime asDate(int timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Convert.fromEpochTime(timestamp)), zoneId);
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            os.write(data, 0, nRead);
        }
        os.flush();
    }


    private static class ArgParser {
        private final String[] params;
        private final List<String> flags;
        private final Map<String, String> additionalParameters;

        private ArgParser(String[] args) {
            final Map<String, List<String>> map = Stream.of(args).collect(Collectors.groupingBy(s -> {
                if (s.startsWith("--")) {
                    return "ADDITIONAL_PARAMS";
                }
                if (s.startsWith("-")) {
                    return "FLAG";
                }
                return "PARAMETER";
            }));
            this.params = map.getOrDefault("PARAMETER", emptyList()).toArray(new String[0]);
            this.flags = map.getOrDefault("FLAG", emptyList());
            this.additionalParameters = split(map.getOrDefault("ADDITIONAL_PARAMS", emptyList()));
        }

        private Map<String, String> split(List<String> list) {
            return list.stream()
                    .map(s -> s.split("="))
                    .collect(Collectors.toMap(arr -> arr[0].substring("--".length()), arr -> arr[1]));
        }

        void parse(Action action) throws IOException, InterruptedException {
            if (params.length < 2 || params.length > 4) {
                System.out.println("" +
                        "This command line utility produces tax report in tab-separated format. Time zone of report will match local.\n" +
                        "Usage: <account> <from-date> [to-date [output-file-name]] [-waitForDownloadComplete] [-ignoreIncompleteDownload]\n" +
                        "\t account\t - account to run report for\n" +
                        "\t from-date\t - date in yyyymmdd format, inclusive\n" +
                        "\t to-date\t - date in yyyymmdd format, inclusive, defaults to today\n" +
                        "\t output-file-name\t - defaults to report-for_account_from-date_to-date.csv\n" +
                        "By default this tool exits with warning if blockchain last block timestamp is before to-date.\n" +
                        "Following flags modify this behavior:\n" +
                        "\t -ignoreIncompleteDownload\t - run report for locally available blockchain\n" +
                        "\t -waitForDownloadComplete\t - run wait for enough blocks downloaded before running this report\n" +
                        "Optional additional parameters\n" +
                        "\t --unknownEventPolicy=<FAIL|IGNORE> - what to do with ledger events unknown by this tool. 'FAIL' by default.\n" +
                        "\t --timeZone=<timezone id> - 'UTC' by default, see java.time.ZoneId.of(java.lang.String) for more info\n" +
                        "\t --dateFormat=<date and time format> - 'dd-MM-yyyy HH:mm:ss' by default, see java.time.format.DateTimeFormatter.ofPattern(java.lang.String) for more info\n" +
                        "\t --delimiter=<delimiter to separate values> - '\\t' by default, no character escaping is done.\n" +
                        "Usage examples:\n" +
                        "ARDOR-4RU9-TNCT-F3MU-8952K 20190101 20200101 output.csv\n" +
                        "ARDOR-4RU9-TNCT-F3MU-8952K 20190101 20200101 \t - will create file with name report-for_ARDOR-4RU9-TNCT-F3MU-8952K_20190101_20200101.csv\n");
                return;
            }
            String account = params[0];
            String fromDate = params[1];
            String toDate = getToDate();
            String fileName = params.length == 4 ? params[3] : String.format("report-for_%s_%s_%s.csv", account, fromDate, toDate);
            boolean waitForDownloadComplete = flags.contains("-waitForDownloadComplete");
            boolean ignoreIncompleteDownload = flags.contains("-ignoreIncompleteDownload");

            try {
                action.doAction(account, fromDate, toDate, fileName, waitForDownloadComplete, ignoreIncompleteDownload, additionalParameters);
            } catch (ExitException e) {
                System.err.println(e.getMessage());
                System.exit(e.exitCode);
            }
        }

        private String getToDate() {
            if (params.length >= 3) {
                return params[2];
            }
            return LocalDate.now(zoneId).plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        }
    }

    private interface Action {
        void doAction(String account, String fromDate, String toDate, String outputFile, boolean waitForDownloadComplete, boolean ignoreIncompleteDownload, Map<String, String> settings) throws IOException, ExitException, InterruptedException;
    }

    private static class ExitException extends Exception {
        private final int exitCode;

        public ExitException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }
    }
}
