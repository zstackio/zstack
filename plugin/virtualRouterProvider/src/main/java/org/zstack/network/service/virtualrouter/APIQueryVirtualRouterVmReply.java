package org.zstack.network.service.virtualrouter;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryVirtualRouterVmReply extends APIQueryReply {
    private List<VirtualRouterVmInventory> inventories;

    public List<VirtualRouterVmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterVmInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVirtualRouterVmReply __example__() {
        APIQueryVirtualRouterVmReply reply = new APIQueryVirtualRouterVmReply();


        return reply;
    }

}
