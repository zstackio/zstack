package org.zstack.sdk;

import org.zstack.sdk.SharedQosInventory;

public class ChangeSharedQosBandwidthResult {
    public SharedQosInventory inventory;
    public void setInventory(SharedQosInventory inventory) {
        this.inventory = inventory;
    }
    public SharedQosInventory getInventory() {
        return this.inventory;
    }

}
