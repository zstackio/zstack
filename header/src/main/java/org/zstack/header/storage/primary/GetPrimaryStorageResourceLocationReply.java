package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2019/6/4.
 */
public class GetPrimaryStorageResourceLocationReply extends MessageReply {
    private String hostUuid;
    private String primaryStorageUuid;
    private String installPath;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
