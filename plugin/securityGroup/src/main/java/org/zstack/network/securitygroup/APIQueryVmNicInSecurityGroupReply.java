package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryVmNicInSecurityGroupReply extends APIQueryReply {
    private List<VmNicSecurityGroupRefInventory> inventories;

    public List<VmNicSecurityGroupRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicSecurityGroupRefInventory> inventories) {
        this.inventories = inventories;
    }
}
