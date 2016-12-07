package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetVmAttachableDataVolumeReply extends APIReply {
    private List<VolumeInventory> inventories;

    public List<VolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeInventory> inventories) {
        this.inventories = inventories;
    }
}
