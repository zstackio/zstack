package org.zstack.header.configuration;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryDiskOfferingReply extends APIQueryReply {
    private List<DiskOfferingInventory> inventories;

    public List<DiskOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DiskOfferingInventory> inventories) {
        this.inventories = inventories;
    }
}
