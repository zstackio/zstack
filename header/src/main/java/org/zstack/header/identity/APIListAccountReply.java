package org.zstack.header.identity;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListAccountReply extends APIReply {
    private List<AccountInventory> inventories;

    public List<AccountInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListAccountReply __example__() {
        APIListAccountReply reply = new APIListAccountReply();


        return reply;
    }

}
