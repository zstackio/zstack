package org.zstack.sdk;



public class RefreshAiHostModelCacheResult {
    public java.util.List<org.zstack.sdk.AiHostCacheStorageInventory> inventories;
    public void setInventories(java.util.List<org.zstack.sdk.AiHostCacheStorageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<org.zstack.sdk.AiHostCacheStorageInventory> getInventories() {
        return this.inventories;
    }

    public java.util.List failureReasons;
    public void setFailureReasons(java.util.List failureReasons) {
        this.failureReasons = failureReasons;
    }
    public java.util.List getFailureReasons() {
        return this.failureReasons;
    }

}
