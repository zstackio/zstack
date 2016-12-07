package org.zstack.header.storage.snapshot;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryVolumeSnapshotTreeReply extends APIQueryReply {
    private List<VolumeSnapshotTreeInventory> inventories;

    public List<VolumeSnapshotTreeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotTreeInventory> inventories) {
        this.inventories = inventories;
    }
}
