package org.zstack.network.service.flat;

/**
 * Created by jianjun.zhang on 2/8/2023
 */
public class NamespaceHelper {

    public static String makeNamespaceName(String l3Uuid, boolean exceptionOnNotFound) {
        String brName = new BridgeNameFinder().findByL3Uuid(l3Uuid, exceptionOnNotFound);
        if (brName != null) {
            return String.format("%s_%s", brName, l3Uuid);
        }
        return null;
    }

    public static String makeNamespaceName(String l3Uuid) {
        String brName = new BridgeNameFinder().findByL3Uuid(l3Uuid);
        return String.format("%s_%s", brName, l3Uuid);
    }

    public static String makeNamespaceName(String brName, String l3Uuid) {
        return String.format("%s_%s", brName, l3Uuid);
    }
}
