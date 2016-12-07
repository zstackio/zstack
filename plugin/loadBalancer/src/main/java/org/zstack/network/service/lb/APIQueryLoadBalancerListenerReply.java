package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 8/18/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryLoadBalancerListenerReply extends APIQueryReply {
    private List<LoadBalancerListenerInventory> inventories;

    public List<LoadBalancerListenerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerListenerInventory> inventories) {
        this.inventories = inventories;
    }
}
