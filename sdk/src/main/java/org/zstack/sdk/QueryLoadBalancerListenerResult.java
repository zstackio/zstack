package org.zstack.sdk;

public class QueryLoadBalancerListenerResult {
    public java.util.List<LoadBalancerListenerInventory> inventories;
    public void setInventories(java.util.List<LoadBalancerListenerInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<LoadBalancerListenerInventory> getInventories() {
        return this.inventories;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

}
