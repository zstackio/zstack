package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse
public class APIQueryUserReply extends APIQueryReply {
    private List<UserInventory> inventories;

    public List<UserInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserInventory> inventories) {
        this.inventories = inventories;
    }
}
