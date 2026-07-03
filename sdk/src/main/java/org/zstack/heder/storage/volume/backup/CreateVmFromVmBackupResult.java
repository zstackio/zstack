package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.VmInstanceInventory;

/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.CreateVmFromVmBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class CreateVmFromVmBackupResult {
    public VmInstanceInventory inventory;
    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstanceInventory getInventory() {
        return this.inventory;
    }

}
