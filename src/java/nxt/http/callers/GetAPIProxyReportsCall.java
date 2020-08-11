// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class GetAPIProxyReportsCall extends APICall.Builder<GetAPIProxyReportsCall> {
    private GetAPIProxyReportsCall() {
        super(ApiSpec.getAPIProxyReports);
    }

    public static GetAPIProxyReportsCall create() {
        return new GetAPIProxyReportsCall();
    }

    public GetAPIProxyReportsCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
