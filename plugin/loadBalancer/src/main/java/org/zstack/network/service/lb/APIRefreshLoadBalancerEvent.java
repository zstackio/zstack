package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/18/2015.
 */
public class APIRefreshLoadBalancerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIRefreshLoadBalancerEvent() {
    }

    public APIRefreshLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
}
