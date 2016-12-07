package org.zstack.network.service.virtualrouter;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVirtualRouterOfferingReply extends APIQueryReply {
    private List<VirtualRouterOfferingInventory> inventories;

    public List<VirtualRouterOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterOfferingInventory> inventories) {
        this.inventories = inventories;
    }
}
