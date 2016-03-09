package org.zstack.storage.primary.iscsi;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 * Created by frank on 4/27/2015.
 */
public class APIQueryIscsiFileSystemBackendPrimaryStorageReply extends APIQueryReply {
    private List<IscsiFileSystemBackendPrimaryStorageInventory> inventories;

    public List<IscsiFileSystemBackendPrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IscsiFileSystemBackendPrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }
}
