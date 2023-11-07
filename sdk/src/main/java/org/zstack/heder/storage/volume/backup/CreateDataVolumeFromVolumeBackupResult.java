package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.VolumeInventory;

public class CreateDataVolumeFromVolumeBackupResult {
    public VolumeInventory inventory;
    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeInventory getInventory() {
        return this.inventory;
    }

}
