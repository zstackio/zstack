package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryUserGroupReply extends APIQueryReply {
    private List<UserGroupInventory> inventories;

    public List<UserGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserGroupInventory> inventories) {
        this.inventories = inventories;
    }
}
