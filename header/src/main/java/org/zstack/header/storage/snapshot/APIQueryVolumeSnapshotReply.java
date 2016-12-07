package org.zstack.header.storage.snapshot;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryVolumeSnapshotReply extends APIQueryReply {
    private List<VolumeSnapshotInventory> inventories;

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }
}
