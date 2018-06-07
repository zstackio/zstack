package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBAC;

public class PolicyUtils {
    public static boolean isAdminOnlyAction(String action) {
        return RBAC.isAdminOnlyAPI(apiNamePatternFromAction(action));
    }

    public static String apiNamePatternFromAction(String action) {
        return action.split(":")[0];
    }
}
