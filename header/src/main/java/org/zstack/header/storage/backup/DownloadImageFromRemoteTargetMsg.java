package org.zstack.header.storage.backup;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.addon.RemoteTarget;

public class DownloadImageFromRemoteTargetMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String imageUuid;
    private ImageInventory image;
    private String backupStorageUuid;
    private RemoteTarget remoteTarget;
    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public RemoteTarget getRemoteTarget() {
        return remoteTarget;
    }

    public void setRemoteTarget(RemoteTarget remoteTarget) {
        this.remoteTarget = remoteTarget;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }
}
