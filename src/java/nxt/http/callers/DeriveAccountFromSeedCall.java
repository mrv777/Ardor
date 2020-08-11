// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class DeriveAccountFromSeedCall extends APICall.Builder<DeriveAccountFromSeedCall> {
    private DeriveAccountFromSeedCall() {
        super(ApiSpec.deriveAccountFromSeed);
    }

    public static DeriveAccountFromSeedCall create() {
        return new DeriveAccountFromSeedCall();
    }

    public DeriveAccountFromSeedCall bip32Path(String bip32Path) {
        return param("bip32Path", bip32Path);
    }

    public DeriveAccountFromSeedCall mnemonic(String mnemonic) {
        return param("mnemonic", mnemonic);
    }

    public DeriveAccountFromSeedCall passphrase(String passphrase) {
        return param("passphrase", passphrase);
    }
}
