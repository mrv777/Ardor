// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class SetConfigurationCall extends APICall.Builder<SetConfigurationCall> {
    private SetConfigurationCall() {
        super(ApiSpec.setConfiguration);
    }

    public static SetConfigurationCall create() {
        return new SetConfigurationCall();
    }

    public SetConfigurationCall propertiesJSON(String propertiesJSON) {
        return param("propertiesJSON", propertiesJSON);
    }

    public SetConfigurationCall shutdown(String shutdown) {
        return param("shutdown", shutdown);
    }

    public SetConfigurationCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
