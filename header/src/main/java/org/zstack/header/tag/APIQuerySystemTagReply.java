package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQuerySystemTagReply extends APIQueryReply {
    private List<SystemTagInventory> inventories;

    public List<SystemTagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SystemTagInventory> inventories) {
        this.inventories = inventories;
    }
}
