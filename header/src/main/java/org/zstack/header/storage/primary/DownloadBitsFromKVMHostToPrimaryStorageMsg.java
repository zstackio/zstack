package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 8/26/18.
 */
public class DownloadBitsFromKVMHostToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String hostUuid;
    private String hostInstallPath;
    private String primaryStorageUuid;
    private String primaryStorageInstallPath;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostInstallPath() {
        return hostInstallPath;
    }

    public void setHostInstallPath(String hostInstallPath) {
        this.hostInstallPath = hostInstallPath;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }
}
