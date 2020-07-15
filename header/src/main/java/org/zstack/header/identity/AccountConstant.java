package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.rest.RestAuthenticationType;

@PythonClass
public interface AccountConstant {
    String SERVICE_ID = "identity";
    @PythonClass
    String INITIAL_SYSTEM_ADMIN_UUID = "36c27e8ff05c4780bf6d2fa65700f22e";
    @PythonClass
    String INITIAL_SYSTEM_ADMIN_NAME = "admin";
    // 'password' SHA512 hex coding
    @PythonClass
    String INITIAL_SYSTEM_ADMIN_PASSWORD = "93085405989dd64b7e665ce09dd0df0d33e9051442951b798ce548f5faded82da1d4f31798651048ec650f284b83ac5530113bb0a5247a69e0db0a73abcd7080";

    String SYSTEM_ADMIN_ROLE = ".*";

    int RESOURCE_PERMISSION_READ = 1;
    int RESOURCE_PERMISSION_WRITE = 2;

    String ACTION_CATEGORY = "identity";
    String READ_PERMISSION_POLICY = "default-read-permission";

    String QUOTA_GLOBAL_CONFIG_CATETORY = "quota";

    String PRINCIPAL_USER = "user";
    String PRINCIPAL_ACCOUNT = "account";

    String LOGIN_TYPE = "account";

    IdentityType identityType = new IdentityType("IAM");

    static boolean isAdminPermission(SessionInventory session) {
        return isAdminPermission(session.getAccountUuid());
    }

    static boolean isAdminPermission(String accountUuid) {
        return INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid);
    }

    static boolean isAdmin(SessionInventory session) {
        return INITIAL_SYSTEM_ADMIN_UUID.equals(session.getAccountUuid()) && INITIAL_SYSTEM_ADMIN_UUID.equals(session.getUserUuid());
    }

    enum Principal {
        Account,
        User,
        Role,
        Group
    }

    String ACCOUNT_REST_AUTH = "OAuth";
    RestAuthenticationType ACCOUNT_REST_AUTHENTICATION_TYPE = new RestAuthenticationType(ACCOUNT_REST_AUTH);
}
