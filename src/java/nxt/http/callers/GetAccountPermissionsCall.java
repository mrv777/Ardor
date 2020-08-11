// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class GetAccountPermissionsCall extends APICall.Builder<GetAccountPermissionsCall> {
    private GetAccountPermissionsCall() {
        super(ApiSpec.getAccountPermissions);
    }

    public static GetAccountPermissionsCall create() {
        return new GetAccountPermissionsCall();
    }

    public static GetAccountPermissionsCall create(int chain) {
        return new GetAccountPermissionsCall().param("chain", chain);
    }

    public GetAccountPermissionsCall requireLastBlock(String requireLastBlock) {
        return param("requireLastBlock", requireLastBlock);
    }

    public GetAccountPermissionsCall account(String account) {
        return param("account", account);
    }

    public GetAccountPermissionsCall account(long account) {
        return unsignedLongParam("account", account);
    }

    public GetAccountPermissionsCall requireBlock(String requireBlock) {
        return param("requireBlock", requireBlock);
    }
}
