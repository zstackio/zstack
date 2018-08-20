package org.zstack.sdk;

import org.zstack.sdk.AutoScalingVmTemplateInventory;

public class CreateAutoScalingVmTemplateResult {
    public AutoScalingVmTemplateInventory inventory;
    public void setInventory(AutoScalingVmTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingVmTemplateInventory getInventory() {
        return this.inventory;
    }

}
