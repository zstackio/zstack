package org.zstack.sdk;

public class QueryNotificationSubscriptionResult {
    public java.util.List<NotificationSubscriptionInventory> inventories;
    public void setInventories(java.util.List<NotificationSubscriptionInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<NotificationSubscriptionInventory> getInventories() {
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
