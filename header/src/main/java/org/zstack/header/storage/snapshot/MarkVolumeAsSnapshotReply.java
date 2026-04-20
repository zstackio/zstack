package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

public class MarkVolumeAsSnapshotReply extends MessageReply {
    private VolumeSnapshotInventory inventory;

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
}
