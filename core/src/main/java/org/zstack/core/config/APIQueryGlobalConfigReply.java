package org.zstack.core.config;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQueryGlobalConfigReply extends APIQueryReply {
    private List<GlobalConfigInventory> inventories;

    public List<GlobalConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GlobalConfigInventory> inventories) {
        this.inventories = inventories;
    }
}
