package org.zstack.sdk;

public class UpdateCephPrimaryStoragePoolResult {
    public CephPrimaryStoragePoolInventory inventory;
    public void setInventory(CephPrimaryStoragePoolInventory inventory) {
        this.inventory = inventory;
    }
    public CephPrimaryStoragePoolInventory getInventory() {
        return this.inventory;
    }

}
