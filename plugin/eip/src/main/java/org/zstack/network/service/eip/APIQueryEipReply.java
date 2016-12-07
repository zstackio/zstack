package org.zstack.network.service.eip;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryEipReply extends APIQueryReply {
    private List<EipInventory> inventories;

    public List<EipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EipInventory> inventories) {
        this.inventories = inventories;
    }
}
