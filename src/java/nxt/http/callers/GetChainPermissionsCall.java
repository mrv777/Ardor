// Auto generated code, do not modify
package nxt.http.callers;

import nxt.http.APICall;

public class GetChainPermissionsCall extends APICall.Builder<GetChainPermissionsCall> {
    private GetChainPermissionsCall() {
        super(ApiSpec.getChainPermissions);
    }

    public static GetChainPermissionsCall create() {
        return new GetChainPermissionsCall();
    }

    public static GetChainPermissionsCall create(int chain) {
        return new GetChainPermissionsCall().param("chain", chain);
    }

    public GetChainPermissionsCall requireLastBlock(String requireLastBlock) {
        return param("requireLastBlock", requireLastBlock);
    }

    public GetChainPermissionsCall firstIndex(int firstIndex) {
        return param("firstIndex", firstIndex);
    }

    public GetChainPermissionsCall permission(String permission) {
        return param("permission", permission);
    }

    public GetChainPermissionsCall lastIndex(int lastIndex) {
        return param("lastIndex", lastIndex);
    }

    public GetChainPermissionsCall granter(String granter) {
        return param("granter", granter);
    }

    public GetChainPermissionsCall requireBlock(String requireBlock) {
        return param("requireBlock", requireBlock);
    }

    public GetChainPermissionsCall adminPassword(String adminPassword) {
        return param("adminPassword", adminPassword);
    }
}
