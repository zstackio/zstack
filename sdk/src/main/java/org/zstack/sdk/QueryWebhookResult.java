package org.zstack.sdk;

public class QueryWebhookResult {
    public java.util.List<WebhookInventory> inventories;
    public void setInventories(java.util.List<WebhookInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<WebhookInventory> getInventories() {
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
