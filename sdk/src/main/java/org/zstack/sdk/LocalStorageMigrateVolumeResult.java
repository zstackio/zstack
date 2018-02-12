package org.zstack.sdk;

public class LocalStorageMigrateVolumeResult {
    public LocalStorageResourceRefInventory inventory;
    public void setInventory(LocalStorageResourceRefInventory inventory) {
        this.inventory = inventory;
    }
    public LocalStorageResourceRefInventory getInventory() {
        return this.inventory;
    }

}
