// Auto generated code, do not modify
package nxt.http.callers;

public class RemoveAccountPermissionCall extends CreateTransactionCallBuilder<RemoveAccountPermissionCall> {
    private RemoveAccountPermissionCall() {
        super(ApiSpec.removeAccountPermission);
    }

    public static RemoveAccountPermissionCall create(int chain) {
        return new RemoveAccountPermissionCall().param("chain", chain);
    }

    public RemoveAccountPermissionCall permission(String permission) {
        return param("permission", permission);
    }

    public RemoveAccountPermissionCall height(int height) {
        return param("height", height);
    }
}
