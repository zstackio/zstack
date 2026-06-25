package org.zstack.sdk;

import org.zstack.sdk.VolumeCacheInventory;

public class EnableVolumeCacheResult {
    public VolumeCacheInventory inventory;
    public void setInventory(VolumeCacheInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeCacheInventory getInventory() {
        return this.inventory;
    }

}
