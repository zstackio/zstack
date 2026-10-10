package org.zstack.sdk;

import org.zstack.sdk.MemoryWritebackBackendInventory;

public class GetHostMemoryWritebackBackendsResult {
    public MemoryWritebackBackendInventory inventory;
    public void setInventory(MemoryWritebackBackendInventory inventory) {
        this.inventory = inventory;
    }
    public MemoryWritebackBackendInventory getInventory() {
        return this.inventory;
    }

}
