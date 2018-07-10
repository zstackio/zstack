package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBACGroovy;

public class PolicyUtils {
    public static boolean isAdminOnlyAction(String action) {
        return RBACGroovy.isAdminOnlyAPI(apiNamePatternFromAction(action));
    }

    public static String apiNamePatternFromAction(String action) {
        return action.split(":")[0];
    }
}
