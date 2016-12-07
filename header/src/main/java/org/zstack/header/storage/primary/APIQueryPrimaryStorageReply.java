package org.zstack.header.storage.primary;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPrimaryStorageReply extends APIQueryReply {
    private List<PrimaryStorageInventory> inventories;

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }
}
