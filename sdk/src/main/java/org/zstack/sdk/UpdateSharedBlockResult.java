package org.zstack.sdk;

import org.zstack.sdk.SharedBlockGroupPrimaryStorageInventory;

public class UpdateSharedBlockResult {
    public SharedBlockGroupPrimaryStorageInventory inventory;
    public void setInventory(SharedBlockGroupPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public SharedBlockGroupPrimaryStorageInventory getInventory() {
        return this.inventory;
    }

}
