package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryUserTagReply extends APIQueryReply {
    private List<UserTagInventory> inventories;

    public List<UserTagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserTagInventory> inventories) {
        this.inventories = inventories;
    }
}
