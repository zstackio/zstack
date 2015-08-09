package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/8/2015.
 */
public class APIRemoveNicFromLoadBalancerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIRemoveNicFromLoadBalancerEvent() {
    }

    public APIRemoveNicFromLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
}
