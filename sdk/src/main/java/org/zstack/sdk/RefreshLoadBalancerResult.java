package org.zstack.sdk;

public class RefreshLoadBalancerResult {
    public LoadBalancerInventory inventory;
    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
    public LoadBalancerInventory getInventory() {
        return this.inventory;
    }

}
