package org.zstack.header.storage.backup;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.image.APIDeleteImageMsg;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
@ApiTimeout(apiClasses = {APIDeleteImageMsg.class, APIDeleteExportedImageFromBackupStorageMsg.class})
public class DeleteBitsOnBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String installPath;

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
}
