package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 */
public class APIQuerySecurityGroupRuleReply extends APIQueryReply {
    private List<SecurityGroupRuleInventory> inventories;

    public List<SecurityGroupRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityGroupRuleInventory> inventories) {
        this.inventories = inventories;
    }
}
