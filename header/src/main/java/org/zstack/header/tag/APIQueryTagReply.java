package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryTagReply extends APIQueryReply {
    private List<TagInventory> inventories;

    public List<TagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TagInventory> inventories) {
        this.inventories = inventories;
    }
}

