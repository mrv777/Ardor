// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class StartContractRunnerEncryptedCall extends APICall.Builder<StartContractRunnerEncryptedCall> {
    private StartContractRunnerEncryptedCall() {
        super(ApiSpec.startContractRunnerEncrypted);
    }

    public static StartContractRunnerEncryptedCall create() {
        return new StartContractRunnerEncryptedCall();
    }

    public StartContractRunnerEncryptedCall path(String path) {
        return param("path", path);
    }

    public StartContractRunnerEncryptedCall encryptionPassword(String encryptionPassword) {
        return param("encryptionPassword", encryptionPassword);
    }
}
