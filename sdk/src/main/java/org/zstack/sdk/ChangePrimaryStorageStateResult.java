package org.zstack.sdk;

public class ChangePrimaryStorageStateResult {
    public PrimaryStorageInventory inventory;
    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public PrimaryStorageInventory getInventory() {
        return this.inventory;
    }

}
