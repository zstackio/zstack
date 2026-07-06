package org.zstack.storage.ceph;

import org.zstack.utils.network.IPv6NetworkUtils;

/**
 * Created by xing5 on 2017/9/22.
 */
public class CephAgentUrl {
    public static String primaryStorageUrl(String ip, String path) {
        return agentUrl(ip, CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT,
                CephGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH, path);
    }

    public static String backupStorageUrl(String ip, String path) {
        return agentUrl(ip, CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT,
                CephGlobalProperty.BACKUP_STORAGE_AGENT_URL_ROOT_PATH, path);
    }

    private static String agentUrl(String ip, int port, String rootPath, String path) {
        StringBuilder ub = new StringBuilder(IPv6NetworkUtils.buildHttpUrl(ip, port));
        if (!"".equals(rootPath)) {
            ub.append(rootPath);
        }
        ub.append(path);
        return ub.toString();
    }
}
