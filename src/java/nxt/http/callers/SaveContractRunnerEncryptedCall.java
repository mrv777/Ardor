// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class SaveContractRunnerEncryptedCall extends APICall.Builder<SaveContractRunnerEncryptedCall> {
    private SaveContractRunnerEncryptedCall() {
        super(ApiSpec.saveContractRunnerEncrypted);
    }

    public static SaveContractRunnerEncryptedCall create() {
        return new SaveContractRunnerEncryptedCall();
    }

    public SaveContractRunnerEncryptedCall path(String path) {
        return param("path", path);
    }

    public SaveContractRunnerEncryptedCall contractRunner(String contractRunner) {
        return param("contractRunner", contractRunner);
    }

    public SaveContractRunnerEncryptedCall dataAlreadyEncrypted(String dataAlreadyEncrypted) {
        return param("dataAlreadyEncrypted", dataAlreadyEncrypted);
    }

    public SaveContractRunnerEncryptedCall encryptionPassword(String encryptionPassword) {
        return param("encryptionPassword", encryptionPassword);
    }

    public SaveContractRunnerEncryptedCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
