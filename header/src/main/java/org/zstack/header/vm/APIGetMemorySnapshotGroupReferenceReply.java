package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;

import java.util.List;

/**
 * Created by LiangHanYu on 2022/7/5 17:45
 */
@RestResponse(allTo = "inventories")
public class APIGetMemorySnapshotGroupReferenceReply extends APIReply {
    private List<VolumeSnapshotGroupInventory> inventories;

    public List<VolumeSnapshotGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetMemorySnapshotGroupReferenceReply __example__() {
        APIGetMemorySnapshotGroupReferenceReply reply = new APIGetMemorySnapshotGroupReferenceReply();
        return reply;
    }

}
