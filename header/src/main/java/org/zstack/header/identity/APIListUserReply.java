package org.zstack.header.identity;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListUserReply extends APIReply {
    private List<UserInventory> inventories;

    public List<UserInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListUserReply __example__() {
        APIListUserReply reply = new APIListUserReply();


        return reply;
    }

}
