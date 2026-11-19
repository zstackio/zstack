package org.zstack.sdk;

import org.zstack.sdk.GpuCapacity;

public class CheckFineTuneGpuCapacityResult {
    public GpuCapacity inventory;
    public void setInventory(GpuCapacity inventory) {
        this.inventory = inventory;
    }
    public GpuCapacity getInventory() {
        return this.inventory;
    }

}
