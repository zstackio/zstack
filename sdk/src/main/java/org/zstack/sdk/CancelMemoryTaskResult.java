package org.zstack.sdk;

import org.zstack.sdk.MemoryTaskInventory;

public class CancelMemoryTaskResult {
    public MemoryTaskInventory inventory;
    public void setInventory(MemoryTaskInventory inventory) {
        this.inventory = inventory;
    }
    public MemoryTaskInventory getInventory() {
        return this.inventory;
    }

}
