package org.zstack.sdk;

public class QueryNotificationResult {
    public java.util.List<NotificationInventory> inventories;
    public void setInventories(java.util.List<NotificationInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<NotificationInventory> getInventories() {
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
