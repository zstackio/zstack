package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryPrimaryStorageReply extends APIQueryReply {
    private List<PrimaryStorageInventory> inventories;

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }
}
