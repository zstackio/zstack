package org.zstack.header.volume;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.message.NeedReplyMessage;

@ApiTimeout(apiClasses = {APICreateDataVolumeTemplateFromVolumeMsg.class})
public class CreateDataVolumeTemplateFromDataVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;
    private String backupStorageUuid;
    private String imageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
