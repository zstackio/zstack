package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class ExportImageToRemoteTargetMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String installPath;
    private String backupStorageUuid;
    private String remoteTargetUrl;
    private String format;
    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getRemoteTargetUrl() {
        return remoteTargetUrl;
    }

    public void setRemoteTargetUrl(String remoteTargetUrl) {
        this.remoteTargetUrl = remoteTargetUrl;
    }
}
