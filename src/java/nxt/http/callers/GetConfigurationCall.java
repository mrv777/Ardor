// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class GetConfigurationCall extends APICall.Builder<GetConfigurationCall> {
    private GetConfigurationCall() {
        super(ApiSpec.getConfiguration);
    }

    public static GetConfigurationCall create() {
        return new GetConfigurationCall();
    }

    public GetConfigurationCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
