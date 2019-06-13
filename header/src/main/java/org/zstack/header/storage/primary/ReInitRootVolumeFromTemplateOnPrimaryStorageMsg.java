package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;


public class ReInitRootVolumeFromTemplateOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volume;
    private long originSize;

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

    public long getOriginSize() {
        return originSize;
    }

    public void setOriginSize(long originSize) {
        this.originSize = originSize;
    }
}
