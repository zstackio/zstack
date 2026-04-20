package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

public class ReInitDataVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volume;
    private String allocatedInstallUrl;

    @Override
    public String getPrimaryStorageUuid() {
        if (volume == null || volume.getPrimaryStorageUuid() == null) {
            throw new IllegalArgumentException("volume and primaryStorageUuid must not be null");
        }
        return volume.getPrimaryStorageUuid();
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
    }
}
