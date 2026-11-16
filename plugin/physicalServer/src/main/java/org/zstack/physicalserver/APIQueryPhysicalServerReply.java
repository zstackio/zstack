package org.zstack.physicalserver;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPhysicalServerReply extends APIQueryReply {
    private List<PhysicalServerInventory> inventories;

    public List<PhysicalServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalServerInventory> inventories) {
        this.inventories = inventories;
    }
}
