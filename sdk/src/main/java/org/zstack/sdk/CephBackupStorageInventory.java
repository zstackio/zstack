package org.zstack.sdk;

public class CephBackupStorageInventory extends BackupStorageInventory {

    public java.util.List<CephBackupStorageMonInventory> mons;
    public void setMons(java.util.List<CephBackupStorageMonInventory> mons) {
        this.mons = mons;
    }
    public java.util.List<CephBackupStorageMonInventory> getMons() {
        return this.mons;
    }

    public java.lang.String fsid;
    public void setFsid(java.lang.String fsid) {
        this.fsid = fsid;
    }
    public java.lang.String getFsid() {
        return this.fsid;
    }

    public java.lang.String poolName;
    public void setPoolName(java.lang.String poolName) {
        this.poolName = poolName;
    }
    public java.lang.String getPoolName() {
        return this.poolName;
    }

}
