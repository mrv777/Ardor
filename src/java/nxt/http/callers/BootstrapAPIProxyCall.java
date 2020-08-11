// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class BootstrapAPIProxyCall extends APICall.Builder<BootstrapAPIProxyCall> {
    private BootstrapAPIProxyCall() {
        super(ApiSpec.bootstrapAPIProxy);
    }

    public static BootstrapAPIProxyCall create() {
        return new BootstrapAPIProxyCall();
    }

    public BootstrapAPIProxyCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
