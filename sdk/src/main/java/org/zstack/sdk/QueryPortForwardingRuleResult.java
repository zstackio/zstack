package org.zstack.sdk;

public class QueryPortForwardingRuleResult {
    public java.util.List<PortForwardingRuleInventory> inventories;
    public void setInventories(java.util.List<PortForwardingRuleInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<PortForwardingRuleInventory> getInventories() {
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
