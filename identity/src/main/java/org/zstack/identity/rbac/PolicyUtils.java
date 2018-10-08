package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBAC;

public class PolicyUtils {
    public static boolean isAdminOnlyAction(String action) {
        return RBAC.isAdminOnlyAPI(apiNamePatternFromAction(action));
    }

    public static String apiNamePatternFromAction(String action) {
        return apiNamePatternFromAction(action, false);
    }

    public static String apiNamePatternFromAction(String action, boolean oldPolicy) {
        if (!oldPolicy) {
            return action.split(":")[0];
        }

        String[] splited = action.split(":");

        if (splited.length != 2) {
            return splited[0];
        } else {
            return splited[1];
        }
    }
}
