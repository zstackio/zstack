package org.zstack.header.host;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryHostReply extends APIQueryReply {
    private List<HostInventory> inventories;

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
}
