package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryVirtualRouterOfferingReply extends APIQueryReply {
    private List<VirtualRouterOfferingInventory> inventories;

    public List<VirtualRouterOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterOfferingInventory> inventories) {
        this.inventories = inventories;
    }
}
