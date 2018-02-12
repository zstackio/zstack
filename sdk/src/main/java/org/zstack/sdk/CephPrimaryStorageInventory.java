package org.zstack.sdk;

public class CephPrimaryStorageInventory extends PrimaryStorageInventory {

    public java.util.List<CephPrimaryStorageMonInventory> mons;
    public void setMons(java.util.List<CephPrimaryStorageMonInventory> mons) {
        this.mons = mons;
    }
    public java.util.List<CephPrimaryStorageMonInventory> getMons() {
        return this.mons;
    }

    public java.util.List<CephPrimaryStoragePoolInventory> pools;
    public void setPools(java.util.List<CephPrimaryStoragePoolInventory> pools) {
        this.pools = pools;
    }
    public java.util.List<CephPrimaryStoragePoolInventory> getPools() {
        return this.pools;
    }

    public java.lang.String fsid;
    public void setFsid(java.lang.String fsid) {
        this.fsid = fsid;
    }
    public java.lang.String getFsid() {
        return this.fsid;
    }

}
