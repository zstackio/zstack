package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryVolumeSnapshotTreeReply extends APIQueryReply {
    private List<VolumeSnapshotTreeInventory> inventories;

    public List<VolumeSnapshotTreeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotTreeInventory> inventories) {
        this.inventories = inventories;
    }
}
