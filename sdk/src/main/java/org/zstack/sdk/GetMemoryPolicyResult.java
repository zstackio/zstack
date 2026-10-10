package org.zstack.sdk;

import org.zstack.sdk.MemoryPolicyInventory;

public class GetMemoryPolicyResult {
    public MemoryPolicyInventory inventory;
    public void setInventory(MemoryPolicyInventory inventory) {
        this.inventory = inventory;
    }
    public MemoryPolicyInventory getInventory() {
        return this.inventory;
    }

}
