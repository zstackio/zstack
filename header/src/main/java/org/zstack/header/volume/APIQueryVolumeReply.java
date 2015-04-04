package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryVolumeReply extends APIQueryReply {
    private List<VolumeInventory> inventories;

    public List<VolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeInventory> inventories) {
        this.inventories = inventories;
    }
}
