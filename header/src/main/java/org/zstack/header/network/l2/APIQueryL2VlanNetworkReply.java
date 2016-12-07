package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL2VlanNetworkReply extends APIQueryReply {
    private List<L2VlanNetworkInventory> inventories;

    public List<L2VlanNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VlanNetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
