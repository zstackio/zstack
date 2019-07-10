package org.zstack.sdk;

import org.zstack.sdk.FlowCollectorInventory;

public class CreateFlowCollectorResult {
    public FlowCollectorInventory inventory;
    public void setInventory(FlowCollectorInventory inventory) {
        this.inventory = inventory;
    }
    public FlowCollectorInventory getInventory() {
        return this.inventory;
    }

}
