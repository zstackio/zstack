package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryManagementNodeReply extends APIQueryReply {
    private List<ManagementNodeInventory> inventories;

    public List<ManagementNodeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ManagementNodeInventory> inventories) {
        this.inventories = inventories;
    }
}
