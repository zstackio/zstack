package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryVmNicReply extends APIQueryReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
}
