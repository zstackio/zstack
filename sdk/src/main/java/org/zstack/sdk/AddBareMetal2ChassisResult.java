package org.zstack.sdk;

import org.zstack.sdk.BareMetal2ChassisInventory;

public class AddBareMetal2ChassisResult {
    public BareMetal2ChassisInventory inventory;
    public void setInventory(BareMetal2ChassisInventory inventory) {
        this.inventory = inventory;
    }
    public BareMetal2ChassisInventory getInventory() {
        return this.inventory;
    }

}
