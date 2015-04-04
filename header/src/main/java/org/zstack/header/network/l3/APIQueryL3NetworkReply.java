package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryL3NetworkReply extends APIQueryReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
