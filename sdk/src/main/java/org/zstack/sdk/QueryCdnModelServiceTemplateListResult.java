package org.zstack.sdk;



public class QueryCdnModelServiceTemplateListResult {
    public java.util.List<org.zstack.sdk.CdnModelServiceTemplateInventory> inventories;
    public void setInventories(java.util.List<org.zstack.sdk.CdnModelServiceTemplateInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<org.zstack.sdk.CdnModelServiceTemplateInventory> getInventories() {
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
