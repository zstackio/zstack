package org.zstack.sdk;

import org.zstack.sdk.SharedQosInventory;

public class CreateSharedQosResult {
    public SharedQosInventory inventory;
    public void setInventory(SharedQosInventory inventory) {
        this.inventory = inventory;
    }
    public SharedQosInventory getInventory() {
        return this.inventory;
    }

}
