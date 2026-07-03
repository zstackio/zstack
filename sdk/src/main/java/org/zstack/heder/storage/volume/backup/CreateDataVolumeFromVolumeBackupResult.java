package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.VolumeInventory;

/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.CreateDataVolumeFromVolumeBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class CreateDataVolumeFromVolumeBackupResult {
    public VolumeInventory inventory;
    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeInventory getInventory() {
        return this.inventory;
    }

}
