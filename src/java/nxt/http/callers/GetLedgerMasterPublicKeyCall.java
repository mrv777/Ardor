// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class GetLedgerMasterPublicKeyCall extends APICall.Builder<GetLedgerMasterPublicKeyCall> {
    private GetLedgerMasterPublicKeyCall() {
        super(ApiSpec.getLedgerMasterPublicKey);
    }

    public static GetLedgerMasterPublicKeyCall create() {
        return new GetLedgerMasterPublicKeyCall();
    }

    public GetLedgerMasterPublicKeyCall bip32Path(String bip32Path) {
        return param("bip32Path", bip32Path);
    }

    public GetLedgerMasterPublicKeyCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
