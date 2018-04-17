package org.zstack.sdk;

import org.zstack.sdk.DahoCloudConnectionInventory;

public class UpdateDahoCloudConnectionResult {
    public DahoCloudConnectionInventory inventory;
    public void setInventory(DahoCloudConnectionInventory inventory) {
        this.inventory = inventory;
    }
    public DahoCloudConnectionInventory getInventory() {
        return this.inventory;
    }

}
