package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/8/2015.
 */
public class APICreateLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APICreateLoadBalancerListenerEvent() {
    }

    public APICreateLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
}
