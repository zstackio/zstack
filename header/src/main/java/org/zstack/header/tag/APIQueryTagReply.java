package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
public class APIQueryTagReply extends APIQueryReply {
    private List<TagInventory> inventories;

    public List<TagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TagInventory> inventories) {
        this.inventories = inventories;
    }
}
 
    public static APIQueryTagReply __example__() {
        APIQueryTagReply reply = new APIQueryTagReply();


        return reply;
    }


