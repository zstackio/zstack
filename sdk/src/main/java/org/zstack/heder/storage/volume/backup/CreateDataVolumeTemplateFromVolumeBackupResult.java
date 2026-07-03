package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.ImageInventory;

/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.CreateDataVolumeTemplateFromVolumeBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class CreateDataVolumeTemplateFromVolumeBackupResult {
    public ImageInventory inventory;
    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
    public ImageInventory getInventory() {
        return this.inventory;
    }

}
