package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 17/2/21.
 */
public class ExportImageFromBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String imageUuid;
    private String rawPath;
    private String exportFormat;
    private Long requiredSize;

    @Override
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

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public String getRawPath() {
        return rawPath;
    }

    public void setRawPath(String rawPath) {
        this.rawPath = rawPath;
    }

    public Long getRequiredSize() {
        return requiredSize;
    }

    public void setRequiredSize(Long requiredSize) {
        this.requiredSize = requiredSize;
    }
}
