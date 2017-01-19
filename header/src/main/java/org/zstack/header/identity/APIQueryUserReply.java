package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryUserReply extends APIQueryReply {
    private List<UserInventory> inventories;

    public List<UserInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryUserReply __example__() {
        APIQueryUserReply reply = new APIQueryUserReply();
        UserInventory inventory = new UserInventory();
        inventory.setName("testuser");
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        reply.setInventories(list(inventory));

        return reply;
    }

}
