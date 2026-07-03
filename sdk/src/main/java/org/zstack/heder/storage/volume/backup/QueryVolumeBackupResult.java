package org.zstack.heder.storage.volume.backup;



/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.QueryVolumeBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class QueryVolumeBackupResult {
    public java.util.List inventories;
    public void setInventories(java.util.List inventories) {
        this.inventories = inventories;
    }
    public java.util.List getInventories() {
        return this.inventories;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

}
