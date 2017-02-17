package org.zstack.sdk;

public class QueryQuotaResult {
    public java.util.List<QuotaInventory> inventories;
    public void setInventories(java.util.List<QuotaInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<QuotaInventory> getInventories() {
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
