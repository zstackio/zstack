package org.zstack.header.resource;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryResourceSourceRefReply extends APIQueryReply {
    private List<ResourceSourceRefInventory> inventories;

    public List<ResourceSourceRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceSourceRefInventory> inventories) {
        this.inventories = inventories;
    }
}
