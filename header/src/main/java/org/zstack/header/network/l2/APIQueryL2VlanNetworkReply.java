package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryL2VlanNetworkReply extends APIQueryReply {
    private List<L2VlanNetworkInventory> inventories;

    public List<L2VlanNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VlanNetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
