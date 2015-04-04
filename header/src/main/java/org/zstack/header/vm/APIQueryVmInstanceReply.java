package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryVmInstanceReply extends APIQueryReply {
    private List<VmInstanceInventory> inventories;

    public List<VmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceInventory> inventories) {
        this.inventories = inventories;
    }
}
