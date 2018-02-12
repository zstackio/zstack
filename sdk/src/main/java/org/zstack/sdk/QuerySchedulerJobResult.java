package org.zstack.sdk;

public class QuerySchedulerJobResult {
    public java.util.List<SchedulerJobInventory> inventories;
    public void setInventories(java.util.List<SchedulerJobInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<SchedulerJobInventory> getInventories() {
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
