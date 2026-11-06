package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBAC;

public class PolicyUtils {
    // Older nodes must not treat a conditional grant as an unconditional API allow.
    public static final String REQUIRED_ACTION_SUFFIX = "@required";

    public static String conditionalAction(String action) {
        return action + REQUIRED_ACTION_SUFFIX;
    }

    public static boolean isConditionalAction(String action) {
        return action.endsWith(REQUIRED_ACTION_SUFFIX);
    }
    public static boolean isAdminOnlyAction(String action) {
        return RBAC.isAdminOnlyAPI(apiNamePatternFromAction(action));
    }

    public static String apiNamePatternFromAction(String action) {
        return apiNamePatternFromAction(action, false);
    }

    public static String apiNamePatternFromAction(String action, boolean oldPolicy) {
        String[] splited = action.split(":");
        String api = oldPolicy && splited.length == 2 ? splited[1] : splited[0];
        return isConditionalAction(api) ? api.substring(0, api.length() - REQUIRED_ACTION_SUFFIX.length()) : api;
    }
}
