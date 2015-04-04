package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

/**
 */
public class UploadVolumeFromPrimaryStorageToBackupStorageReply extends MessageReply {
    private String backupStorageIntallPath;

    public String getBackupStorageIntallPath() {
        return backupStorageIntallPath;
    }

    public void setBackupStorageIntallPath(String backupStorageIntallPath) {
        this.backupStorageIntallPath = backupStorageIntallPath;
    }
}
