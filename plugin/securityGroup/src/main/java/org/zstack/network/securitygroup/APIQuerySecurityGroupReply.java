package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQuerySecurityGroupReply extends APIQueryReply {
    private List<SecurityGroupInventory> inventories;

    public List<SecurityGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityGroupInventory> inventories) {
        this.inventories = inventories;
    }
}
