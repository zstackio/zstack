package org.zstack.sdk;

import org.zstack.sdk.DGpuDeviceInventory;

public class AttachDGpuToVmResult {
    public DGpuDeviceInventory inventory;
    public void setInventory(DGpuDeviceInventory inventory) {
        this.inventory = inventory;
    }
    public DGpuDeviceInventory getInventory() {
        return this.inventory;
    }

}
