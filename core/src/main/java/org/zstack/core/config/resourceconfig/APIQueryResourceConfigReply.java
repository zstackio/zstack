package org.zstack.core.config.resourceconfig;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryResourceConfigReply extends APIQueryReply {
    private List<ResourceConfigInventory> inventories;

    public List<ResourceConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceConfigInventory> inventories) {
        this.inventories = inventories;
    }
}
