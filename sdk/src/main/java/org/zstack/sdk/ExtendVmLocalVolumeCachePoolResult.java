package org.zstack.sdk;

import org.zstack.sdk.VmLocalVolumeCachePoolInventory;

public class ExtendVmLocalVolumeCachePoolResult {
    public VmLocalVolumeCachePoolInventory inventory;
    public void setInventory(VmLocalVolumeCachePoolInventory inventory) {
        this.inventory = inventory;
    }
    public VmLocalVolumeCachePoolInventory getInventory() {
        return this.inventory;
    }

}
