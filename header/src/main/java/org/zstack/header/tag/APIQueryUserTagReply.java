package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryUserTagReply extends APIQueryReply {
    private List<UserTagInventory> inventories;

    public List<UserTagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserTagInventory> inventories) {
        this.inventories = inventories;
    }
}
