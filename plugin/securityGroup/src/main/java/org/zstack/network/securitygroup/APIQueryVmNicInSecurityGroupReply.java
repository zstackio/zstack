package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVmNicInSecurityGroupReply extends APIQueryReply {
    private List<VmNicSecurityGroupRefInventory> inventories;

    public List<VmNicSecurityGroupRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicSecurityGroupRefInventory> inventories) {
        this.inventories = inventories;
    }
}
