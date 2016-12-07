package org.zstack.header.managementnode;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryManagementNodeReply extends APIQueryReply {
    private List<ManagementNodeInventory> inventories;

    public List<ManagementNodeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ManagementNodeInventory> inventories) {
        this.inventories = inventories;
    }
}
