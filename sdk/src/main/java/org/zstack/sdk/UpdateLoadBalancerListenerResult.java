package org.zstack.sdk;

public class UpdateLoadBalancerListenerResult {
    public LoadBalancerListenerInventory inventory;
    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }
    public LoadBalancerListenerInventory getInventory() {
        return this.inventory;
    }

}
