// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class TaxReportCall extends APICall.Builder<TaxReportCall> {
    private TaxReportCall() {
        super(ApiSpec.taxReport);
    }

    public static TaxReportCall create() {
        return new TaxReportCall();
    }

    public TaxReportCall fromDate(String fromDate) {
        return param("fromDate", fromDate);
    }

    public TaxReportCall fromHeight(int fromHeight) {
        return param("fromHeight", fromHeight);
    }

    public TaxReportCall dateFormat(String dateFormat) {
        return param("dateFormat", dateFormat);
    }

    public TaxReportCall delimiter(String delimiter) {
        return param("delimiter", delimiter);
    }

    public TaxReportCall unknownEventPolicy(String unknownEventPolicy) {
        return param("unknownEventPolicy", unknownEventPolicy);
    }

    public TaxReportCall toDate(String toDate) {
        return param("toDate", toDate);
    }

    public TaxReportCall timeZone(String timeZone) {
        return param("timeZone", timeZone);
    }

    public TaxReportCall toHeight(int toHeight) {
        return param("toHeight", toHeight);
    }

    public TaxReportCall account(String... account) {
        return param("account", account);
    }

    public TaxReportCall account(long... account) {
        return unsignedLongParam("account", account);
    }

    public TaxReportCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
