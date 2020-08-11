// Auto generated code, do not modify
package nxt.http.callers;

public class AddAccountPermissionCall extends CreateTransactionCallBuilder<AddAccountPermissionCall> {
    private AddAccountPermissionCall() {
        super(ApiSpec.addAccountPermission);
    }

    public static AddAccountPermissionCall create(int chain) {
        return new AddAccountPermissionCall().param("chain", chain);
    }

    public AddAccountPermissionCall permission(String permission) {
        return param("permission", permission);
    }
}
