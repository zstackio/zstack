package org.zstack.sdk;

import org.zstack.sdk.MemorySwapDiskInventory;

public class GetHostMemorySwapDisksResult {
    public MemorySwapDiskInventory inventory;
    public void setInventory(MemorySwapDiskInventory inventory) {
        this.inventory = inventory;
    }
    public MemorySwapDiskInventory getInventory() {
        return this.inventory;
    }

}
