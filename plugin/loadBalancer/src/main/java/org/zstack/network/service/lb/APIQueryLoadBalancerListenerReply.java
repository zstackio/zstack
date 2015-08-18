package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 * Created by frank on 8/18/2015.
 */
public class APIQueryLoadBalancerListenerReply extends APIQueryReply {
    private List<LoadBalancerListenerInventory> inventories;

    public List<LoadBalancerListenerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerListenerInventory> inventories) {
        this.inventories = inventories;
    }
}
