package org.zstack.sdk;



public class SyncCdnModelServiceTemplateResult {
    public int syncedCount;
    public void setSyncedCount(int syncedCount) {
        this.syncedCount = syncedCount;
    }
    public int getSyncedCount() {
        return this.syncedCount;
    }

    public java.util.List<org.zstack.sdk.CdnModelServiceTemplateInventory> templates;
    public void setTemplates(java.util.List<org.zstack.sdk.CdnModelServiceTemplateInventory> templates) {
        this.templates = templates;
    }
    public java.util.List<org.zstack.sdk.CdnModelServiceTemplateInventory> getTemplates() {
        return this.templates;
    }

}
