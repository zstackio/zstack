package org.zstack.physicalserver;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPhysicalServerResourceAssignmentReply extends APIQueryReply {
    private List<PhysicalServerResourceAssignmentInventory> inventories;

    public List<PhysicalServerResourceAssignmentInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalServerResourceAssignmentInventory> inventories) {
        this.inventories = inventories;
    }
}
