package org.zstack.sdk;

import org.zstack.sdk.SlbGroupInventory;

public class ProvisionSlbGroupInstanceResult {
    public SlbGroupInventory inventory;
    public void setInventory(SlbGroupInventory inventory) {
        this.inventory = inventory;
    }
    public SlbGroupInventory getInventory() {
        return this.inventory;
    }

}
