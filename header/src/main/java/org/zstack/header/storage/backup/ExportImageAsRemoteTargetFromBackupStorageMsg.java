package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 15:13 2023/10/11
 */
public class ExportImageAsRemoteTargetFromBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    private String imageUuid;

    private RemoteTargetType targetType;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public RemoteTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(RemoteTargetType targetType) {
        this.targetType = targetType;
    }
}
