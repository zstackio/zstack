package org.zstack.network.service.eip;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryEipReply extends APIQueryReply {
    private List<EipInventory> inventories;

    public List<EipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EipInventory> inventories) {
        this.inventories = inventories;
    }
}
