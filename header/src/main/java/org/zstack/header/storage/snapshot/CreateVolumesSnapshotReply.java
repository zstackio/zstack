package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Create by weiwang at 2018/6/11
 */
public class CreateVolumesSnapshotReply extends MessageReply {
    private List<VolumeSnapshotInventory> inventories;

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }
}
