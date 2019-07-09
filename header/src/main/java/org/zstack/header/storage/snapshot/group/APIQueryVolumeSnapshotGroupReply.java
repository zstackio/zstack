package org.zstack.header.storage.snapshot.group;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by MaJin on 2019/7/11.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVolumeSnapshotGroupReply extends APIQueryReply {
    private List<VolumeSnapshotGroupInventory> inventories;

    public List<VolumeSnapshotGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotGroupInventory> inventories) {
        this.inventories = inventories;
    }
}
