package org.zstack.appliancevm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryApplianceVmReply extends APIQueryReply {
    private List<ApplianceVmInventory> inventories;

    public List<ApplianceVmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ApplianceVmInventory> inventories) {
        this.inventories = inventories;
    }
}
