package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQuerySecurityGroupRuleReply extends APIQueryReply {
    private List<SecurityGroupRuleInventory> inventories;

    public List<SecurityGroupRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityGroupRuleInventory> inventories) {
        this.inventories = inventories;
    }
}
