package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 2/23/2016.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySharedResourceReply extends APIQueryReply {
    private List<SharedResourceInventory> inventories;

    public List<SharedResourceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SharedResourceInventory> inventories) {
        this.inventories = inventories;
    }
}
