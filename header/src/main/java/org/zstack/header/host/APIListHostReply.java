package org.zstack.header.host;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListHostReply extends APIReply {
    private List<HostInventory> inventories;

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListHostReply __example__() {
        APIListHostReply reply = new APIListHostReply();


        return reply;
    }

}
