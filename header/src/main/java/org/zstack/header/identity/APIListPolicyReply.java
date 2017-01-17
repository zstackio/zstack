package org.zstack.header.identity;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListPolicyReply extends APIReply {
    private List<PolicyInventory> inventories;

    public List<PolicyInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PolicyInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListPolicyReply __example__() {
        APIListPolicyReply reply = new APIListPolicyReply();


        return reply;
    }

}
