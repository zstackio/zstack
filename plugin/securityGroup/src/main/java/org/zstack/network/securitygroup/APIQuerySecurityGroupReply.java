package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySecurityGroupReply extends APIQueryReply {
    private List<SecurityGroupInventory> inventories;

    public List<SecurityGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityGroupInventory> inventories) {
        this.inventories = inventories;
    }
}
