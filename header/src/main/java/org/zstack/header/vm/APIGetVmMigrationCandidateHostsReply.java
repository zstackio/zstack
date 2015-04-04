package org.zstack.header.vm;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;

import java.util.List;

/**
 */
public class APIGetVmMigrationCandidateHostsReply extends APIReply {
    private List<HostInventory> inventories;

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
}
