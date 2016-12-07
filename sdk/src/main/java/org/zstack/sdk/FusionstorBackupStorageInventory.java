package org.zstack.sdk;

public class FusionstorBackupStorageInventory extends BackupStorageInventory {

    public java.util.List<FusionstorBackupStorageMonInventory> mons;
    public void setMons(java.util.List<FusionstorBackupStorageMonInventory> mons) {
        this.mons = mons;
    }
    public java.util.List<FusionstorBackupStorageMonInventory> getMons() {
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

    public java.lang.Integer sshPort;
    public void setSshPort(java.lang.Integer sshPort) {
        this.sshPort = sshPort;
    }
    public java.lang.Integer getSshPort() {
        return this.sshPort;
    }

}
