package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 */
public class APIListManagementNodeReply extends APIReply {
    private List<ManagementNodeInventory> inventories;

    public List<ManagementNodeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ManagementNodeInventory> inventories) {
        this.inventories = inventories;
    }
}
