package org.zstack.sdk;

import org.zstack.sdk.ExternalPrimaryStorageInventory;

public class UpdateExternalPrimaryStorageResult {
    public ExternalPrimaryStorageInventory inventory;
    public void setInventory(ExternalPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalPrimaryStorageInventory getInventory() {
        return this.inventory;
    }

}
