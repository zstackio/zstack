package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryInstanceOfferingReply extends APIQueryReply {
    private List<InstanceOfferingInventory> inventories;

    public List<InstanceOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InstanceOfferingInventory> inventories) {
        this.inventories = inventories;
    }
}
