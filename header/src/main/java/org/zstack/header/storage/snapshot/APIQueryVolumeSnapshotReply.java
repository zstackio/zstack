package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryVolumeSnapshotReply extends APIQueryReply {
    private List<VolumeSnapshotInventory> inventories;

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }
}
