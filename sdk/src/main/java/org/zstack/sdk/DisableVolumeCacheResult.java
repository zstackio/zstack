package org.zstack.sdk;

import org.zstack.sdk.VmLocalVolumeCacheInventory;

public class DisableVolumeCacheResult {
    public VmLocalVolumeCacheInventory inventory;
    public void setInventory(VmLocalVolumeCacheInventory inventory) {
        this.inventory = inventory;
    }
    public VmLocalVolumeCacheInventory getInventory() {
        return this.inventory;
    }

}
