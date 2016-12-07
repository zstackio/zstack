package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetCandidateVmNicForSecurityGroupReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
}
