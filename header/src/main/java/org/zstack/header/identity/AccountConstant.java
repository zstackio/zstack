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
    String INITIAL_SYSTEM_ADMIN_PASSWORD = "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

    String SYSTEM_ADMIN_ROLE = ".*";

    int RESOURCE_PERMISSION_READ = 1;
    int RESOURCE_PERMISSION_WRITE = 2;

    String ACTION_CATEGORY = "identity";
    String READ_PERMISSION_POLICY = "default-read-permission";

    String QUOTA_GLOBAL_CONFIG_CATETORY = "quota";

    String PRINCIPAL_USER = "user";
    String PRINCIPAL_ACCOUNT = "account";

    String LOGIN_TYPE = "account";
    String LOGIN_TYPE_AUTHENTICATIONS_KEY = "authentications";

    String NO_EXIST_ACCOUNT ="no-exist-account:::%s";

    IdentityType identityType = new IdentityType("IAM");

    // login property accountType
    String ACCOUNT_TYPE = "accountType";

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
