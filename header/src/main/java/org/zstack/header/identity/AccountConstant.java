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
    String INITIAL_SYSTEM_ADMIN_PASSWORD = "907c46abff24cc738e71cbb31d09ad0e944a88dab653dd064c640d8d06b3e4c5ff4aebcf8274afa5e7a2ad9074698e5e5c93316c6d248b06e2857b6eae4a8aae";

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
