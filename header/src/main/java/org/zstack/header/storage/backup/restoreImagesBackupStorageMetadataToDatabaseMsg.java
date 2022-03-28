package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class restoreImagesBackupStorageMetadataToDatabaseMsg extends NeedReplyMessage {
    String imagesMetadata;
    String backupStorageUuid;

    public String getImagesMetadata() {
        return imagesMetadata;
    }

    public void setImagesMetadata(String imagesMetadata) {
        this.imagesMetadata = imagesMetadata;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
