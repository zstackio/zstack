package org.zstack.storage.ceph;

import org.zstack.utils.URLBuilder;

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
        if (rootPath == null || "".equals(rootPath)) {
            return URLBuilder.buildHttpUrl(ip, port, path);
        }
        return URLBuilder.buildHttpUrl(ip, port, rootPath, path);
    }
}
