package org.zstack.sdk;

import org.zstack.sdk.VCenterInventory;

public class AddVCenterResult {
    public VCenterInventory inventory;
    public void setInventory(VCenterInventory inventory) {
        this.inventory = inventory;
    }
    public VCenterInventory getInventory() {
        return this.inventory;
    }

}
