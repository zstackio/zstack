package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBAC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.BASE_PACKAGE_NAME;

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

    /**
     * Example:<br/>
     *
     * input: "org.zstack.header.vm.APIStartVmInstanceMsg"<br/>
     * output [".**", ".header.**", ".header.vm.**", ".header.vm.*", ".header.vm.APIStartVmInstanceMsg"]<br/>
     */
    public static List<String> findAllMatchedApiPatterns(String api) {
        List<String> results = new ArrayList<>();

        if (!api.startsWith(BASE_PACKAGE_NAME)) {
            results.add(api);
            return results;
        }

        api = api.substring(BASE_PACKAGE_NAME.length()); // start without "."
        String[] split = api.split("\\.");
        results.add(".**");
        for (int i = 1; i < split.length; i++) {
            results.add("." + String.join(".", Arrays.copyOfRange(split, 0, i)) + ".**");
        }
        results.add("." + String.join(".", Arrays.copyOfRange(split, 0, split.length - 1)) + ".*");
        results.add("." + api);
        return results;
    }
}
