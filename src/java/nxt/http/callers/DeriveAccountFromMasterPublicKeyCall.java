// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class DeriveAccountFromMasterPublicKeyCall extends APICall.Builder<DeriveAccountFromMasterPublicKeyCall> {
    private DeriveAccountFromMasterPublicKeyCall() {
        super(ApiSpec.deriveAccountFromMasterPublicKey);
    }

    public static DeriveAccountFromMasterPublicKeyCall create() {
        return new DeriveAccountFromMasterPublicKeyCall();
    }

    public DeriveAccountFromMasterPublicKeyCall serializedMasterPublicKey(
            String serializedMasterPublicKey) {
        return param("serializedMasterPublicKey", serializedMasterPublicKey);
    }

    public DeriveAccountFromMasterPublicKeyCall serializedMasterPublicKey(
            byte[] serializedMasterPublicKey) {
        return param("serializedMasterPublicKey", serializedMasterPublicKey);
    }

    public DeriveAccountFromMasterPublicKeyCall childIndex(int childIndex) {
        return param("childIndex", childIndex);
    }
}
