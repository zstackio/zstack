package org.zstack.header.storage.backup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryBackupStorageReply extends APIQueryReply {
    private List<BackupStorageInventory> inventories;

    public List<BackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
}
