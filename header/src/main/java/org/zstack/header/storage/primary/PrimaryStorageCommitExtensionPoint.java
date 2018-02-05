package org.zstack.header.storage.primary;

/**
 * Created by mingjian.deng on 2017/10/25.
 */
public interface PrimaryStorageCommitExtensionPoint {
    String getCommitAgentPath(String psType);
    String getHostName(String bsUuid);
}
