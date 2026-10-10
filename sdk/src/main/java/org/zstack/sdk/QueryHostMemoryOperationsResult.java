package org.zstack.sdk;

import org.zstack.sdk.MemoryHostOperationsInventory;

public class QueryHostMemoryOperationsResult {
    public MemoryHostOperationsInventory inventory;
    public void setInventory(MemoryHostOperationsInventory inventory) {
        this.inventory = inventory;
    }
    public MemoryHostOperationsInventory getInventory() {
        return this.inventory;
    }

}
