package org.zstack.sdk;

import org.zstack.sdk.MemoryVmAccountingInventory;

public class GetVmMemoryOptimizationResult {
    public MemoryVmAccountingInventory inventory;
    public void setInventory(MemoryVmAccountingInventory inventory) {
        this.inventory = inventory;
    }
    public MemoryVmAccountingInventory getInventory() {
        return this.inventory;
    }

}
