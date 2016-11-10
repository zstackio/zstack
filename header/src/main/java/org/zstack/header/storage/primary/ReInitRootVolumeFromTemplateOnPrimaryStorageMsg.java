package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;


public class ReInitRootVolumeFromTemplateOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volume;

    @Override
    public String getPrimaryStorageUuid() {
        return volume.getPrimaryStorageUuid();
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
