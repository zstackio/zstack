package org.zstack.heder.storage.volume.backup;



/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.RecoverVmBackupFromImageStoreBackupStorageResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class RecoverVmBackupFromImageStoreBackupStorageResult {
    public java.util.List inventories;
    public void setInventories(java.util.List inventories) {
        this.inventories = inventories;
    }
    public java.util.List getInventories() {
        return this.inventories;
    }

}
