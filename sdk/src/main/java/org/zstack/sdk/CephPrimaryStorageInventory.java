package org.zstack.sdk;

public class CephPrimaryStorageInventory extends PrimaryStorageInventory {

    public java.util.List<CephPrimaryStorageMonInventory> mons;
    public void setMons(java.util.List<CephPrimaryStorageMonInventory> mons) {
        this.mons = mons;
    }
    public java.util.List<CephPrimaryStorageMonInventory> getMons() {
        return this.mons;
    }

    public java.lang.String fsid;
    public void setFsid(java.lang.String fsid) {
        this.fsid = fsid;
    }
    public java.lang.String getFsid() {
        return this.fsid;
    }

    public java.lang.String rootVolumePoolName;
    public void setRootVolumePoolName(java.lang.String rootVolumePoolName) {
        this.rootVolumePoolName = rootVolumePoolName;
    }
    public java.lang.String getRootVolumePoolName() {
        return this.rootVolumePoolName;
    }

    public java.lang.String dataVolumePoolName;
    public void setDataVolumePoolName(java.lang.String dataVolumePoolName) {
        this.dataVolumePoolName = dataVolumePoolName;
    }
    public java.lang.String getDataVolumePoolName() {
        return this.dataVolumePoolName;
    }

    public java.lang.String imageCachePoolName;
    public void setImageCachePoolName(java.lang.String imageCachePoolName) {
        this.imageCachePoolName = imageCachePoolName;
    }
    public java.lang.String getImageCachePoolName() {
        return this.imageCachePoolName;
    }

}
