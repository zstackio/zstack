package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.VolumeBackupInventory;

public class RecoverBackupFromImageStoreBackupStorageResult {
    public VolumeBackupInventory inventory;
    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeBackupInventory getInventory() {
        return this.inventory;
    }

}
