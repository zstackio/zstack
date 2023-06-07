package org.zstack.sdk;

import org.zstack.sdk.StorageEncryptGatewayInventory;

public class UpdateStorageEncryptGatewayResult {
    public StorageEncryptGatewayInventory inventory;
    public void setInventory(StorageEncryptGatewayInventory inventory) {
        this.inventory = inventory;
    }
    public StorageEncryptGatewayInventory getInventory() {
        return this.inventory;
    }

}
