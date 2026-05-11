package org.zstack.sdk;



public class QueryCdnModelServiceTemplateListResult {
    public java.util.List inventories;
    public void setInventories(java.util.List inventories) {
        this.inventories = inventories;
        this.templates = inventories;
    }
    public java.util.List getInventories() {
        return this.inventories;
    }

    public java.util.List templates;
    public void setTemplates(java.util.List templates) {
        this.templates = templates;
        this.inventories = templates;
    }
    public java.util.List getTemplates() {
        return this.templates == null ? this.inventories : this.templates;
    }

}
