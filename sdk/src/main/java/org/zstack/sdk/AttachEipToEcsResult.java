package org.zstack.sdk;

public class AttachEipToEcsResult {
    public HybridEipAddressInventory inventory;
    public void setInventory(HybridEipAddressInventory inventory) {
        this.inventory = inventory;
    }
    public HybridEipAddressInventory getInventory() {
        return this.inventory;
    }

}
