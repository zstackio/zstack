package org.zstack.network.service.lb;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Created by xing5 on 2016/11/29.
 */
@RestResponse(allTo = "inventories")
public class APIGetCandidateVmNicsForLoadBalancerReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
}
