package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

/**
 */
public class CreateDataVolumeFromVolumeSnapshotReply extends MessageReply {
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
