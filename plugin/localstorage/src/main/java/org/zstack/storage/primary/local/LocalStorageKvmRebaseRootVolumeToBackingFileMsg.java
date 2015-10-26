package org.zstack.storage.primary.local;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by frank on 10/26/2015.
 */
public class LocalStorageKvmRebaseRootVolumeToBackingFileMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String hostUuid;
    private VolumeInventory rootVolume;
    private String imageUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VolumeInventory getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(VolumeInventory rootVolume) {
        this.rootVolume = rootVolume;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
