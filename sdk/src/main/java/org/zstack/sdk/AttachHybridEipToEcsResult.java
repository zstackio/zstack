package org.zstack.sdk;

import org.zstack.sdk.HybridEipAddressInventory;

public class AttachHybridEipToEcsResult {
    public HybridEipAddressInventory inventory;
    public void setInventory(HybridEipAddressInventory inventory) {
        this.inventory = inventory;
    }
    public HybridEipAddressInventory getInventory() {
        return this.inventory;
    }

}
