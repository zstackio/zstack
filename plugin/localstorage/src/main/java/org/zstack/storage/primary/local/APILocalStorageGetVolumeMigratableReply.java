package org.zstack.storage.primary.local;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * Created by frank on 11/18/2015.
 */
public class APILocalStorageGetVolumeMigratableReply extends APIReply {
    private List<HostInventory> inventories;

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
}
