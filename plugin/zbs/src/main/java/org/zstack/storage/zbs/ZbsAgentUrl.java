package org.zstack.storage.zbs;

import org.zstack.utils.network.IPv6NetworkUtils;

/**
 * @author Xingwei Yu
 * @date 2024/3/27 17:39
 */
public class ZbsAgentUrl {
    private static void appendPath(StringBuilder sb, String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);
    }

    public static String primaryStorageUrl(String ip, String path) {
        StringBuilder sb = new StringBuilder(IPv6NetworkUtils.buildHttpUrl(ip, ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_PORT));
        appendPath(sb, ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH);
        appendPath(sb, path);
        return sb.toString();
    }
}
