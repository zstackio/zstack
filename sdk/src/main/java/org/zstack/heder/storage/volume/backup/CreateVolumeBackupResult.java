package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.VolumeBackupInventory;

/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.CreateVolumeBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class CreateVolumeBackupResult {
    public VolumeBackupInventory inventory;
    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeBackupInventory getInventory() {
        return this.inventory;
    }

}
