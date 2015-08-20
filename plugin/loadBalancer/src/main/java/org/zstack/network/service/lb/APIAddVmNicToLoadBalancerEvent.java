package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/8/2015.
 */
public class APIAddVmNicToLoadBalancerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIAddVmNicToLoadBalancerEvent() {
    }

    public APIAddVmNicToLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }
}
