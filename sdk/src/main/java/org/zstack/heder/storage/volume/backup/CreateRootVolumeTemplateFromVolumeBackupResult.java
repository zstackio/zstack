package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.ImageInventory;

/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.CreateRootVolumeTemplateFromVolumeBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class CreateRootVolumeTemplateFromVolumeBackupResult {
    public ImageInventory inventory;
    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
    public ImageInventory getInventory() {
        return this.inventory;
    }

}
