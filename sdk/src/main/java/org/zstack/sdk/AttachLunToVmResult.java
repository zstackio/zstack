package org.zstack.sdk;

import org.zstack.sdk.LunInventory;

public class AttachLunToVmResult {
    public LunInventory inventory;
    public void setInventory(LunInventory inventory) {
        this.inventory = inventory;
    }
    public LunInventory getInventory() {
        return this.inventory;
    }

}
