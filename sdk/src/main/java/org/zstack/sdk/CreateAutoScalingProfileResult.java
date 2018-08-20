package org.zstack.sdk;

import org.zstack.sdk.AutoScalingProfileInventory;

public class CreateAutoScalingProfileResult {
    public AutoScalingProfileInventory inventory;
    public void setInventory(AutoScalingProfileInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingProfileInventory getInventory() {
        return this.inventory;
    }

}
