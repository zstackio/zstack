package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL2NetworkReply extends APIQueryReply {
    private List<L2NetworkInventory> inventories;

    public List<L2NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2NetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
