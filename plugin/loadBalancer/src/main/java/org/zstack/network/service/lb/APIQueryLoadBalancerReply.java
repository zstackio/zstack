package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 * Created by frank on 8/18/2015.
 */
public class APIQueryLoadBalancerReply extends APIQueryReply {
    private List<LoadBalancerInventory> inventories;

    public List<LoadBalancerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerInventory> inventories) {
        this.inventories = inventories;
    }
}
