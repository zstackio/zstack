package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2018/1/12.
 */
public class GetInstallPathForDataVolumeDownloadMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private ImageBackupStorageRefInventory backupStorageRef;
    private ImageInventory image;
    private String volumeUuid;
    private String hostUuid;
    private String allocatedUrl;

    public String getAllocatedUrl() {
        return allocatedUrl;
    }

    public void setAllocatedUrl(String allocatedUrl) {
        this.allocatedUrl = allocatedUrl;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public ImageBackupStorageRefInventory getBackupStorageRef() {
        return backupStorageRef;
    }

    public void setBackupStorageRef(ImageBackupStorageRefInventory backupStorageRef) {
        this.backupStorageRef = backupStorageRef;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }
}
