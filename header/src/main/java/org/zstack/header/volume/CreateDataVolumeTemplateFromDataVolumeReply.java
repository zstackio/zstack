package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 */
public class CreateDataVolumeTemplateFromDataVolumeReply extends MessageReply {
    private String backupStorageUuid;
    private String installPath;
    private String md5sum;

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

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }
}
