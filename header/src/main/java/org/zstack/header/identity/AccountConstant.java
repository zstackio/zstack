package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface AccountConstant {
    public static final String SERVICE_ID = "identity";
    @PythonClass
    public static final String INITIAL_SYSTEM_ADMIN_UUID = "36c27e8ff05c4780bf6d2fa65700f22e";
    @PythonClass
    public static final String INITIAL_SYSTEM_ADMIN_NAME = "admin";
    // 'password' SHA512 hex coding
    @PythonClass
    public static final String INITIAL_SYSTEM_ADMIN_PASSWORD = "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";
    
    public static final String SYSTEM_ADMIN_ROLE = ".*";
    
    public static final int RESOURCE_PERMISSION_READ = 1;
    public static final int RESOURCE_PERMISSION_WRITE = 2;

    public static final String ACTION_CATEGORY = "identity";
    public static final String READ_PERMISSION_POLICY = "default-read-permission";

    public static final String QUOTA_GLOBAL_CONFIG_CATETORY = "quota";
    
    public static enum RoleDecision {
        EXPLICIT_DENY,
        DEFAULT_DENY,
        DENY,
        ALLOW,
    }
    
    public static enum StatementEffect {
        Allow,
        Deny,
    }

    public static enum Principal {
        Account,
        User,
        Role,
        Group
    }
}
