package org.zstack.appliancevm;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryApplianceVmReply extends APIQueryReply {
    private List<ApplianceVmInventory> inventories;

    public List<ApplianceVmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ApplianceVmInventory> inventories) {
        this.inventories = inventories;
    }
}
