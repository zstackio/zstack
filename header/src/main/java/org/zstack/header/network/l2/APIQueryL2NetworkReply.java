package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryL2NetworkReply extends APIQueryReply {
    private List<L2NetworkInventory> inventories;

    public List<L2NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2NetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
