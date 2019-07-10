package org.zstack.sdk;

import org.zstack.sdk.FlowMeterInventory;

public class CreateFlowMeterResult {
    public FlowMeterInventory inventory;
    public void setInventory(FlowMeterInventory inventory) {
        this.inventory = inventory;
    }
    public FlowMeterInventory getInventory() {
        return this.inventory;
    }

}
