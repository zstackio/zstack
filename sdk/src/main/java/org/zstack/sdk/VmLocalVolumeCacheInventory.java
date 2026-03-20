package org.zstack.sdk;

import org.zstack.sdk.VmLocalVolumeCacheMode;
import org.zstack.sdk.VmLocalVolumeCacheState;

public class VmLocalVolumeCacheInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public java.lang.String poolUuid;
    public void setPoolUuid(java.lang.String poolUuid) {
        this.poolUuid = poolUuid;
    }
    public java.lang.String getPoolUuid() {
        return this.poolUuid;
    }

    public java.lang.String installPath;
    public void setInstallPath(java.lang.String installPath) {
        this.installPath = installPath;
    }
    public java.lang.String getInstallPath() {
        return this.installPath;
    }

    public VmLocalVolumeCacheMode cacheMode;
    public void setCacheMode(VmLocalVolumeCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }
    public VmLocalVolumeCacheMode getCacheMode() {
        return this.cacheMode;
    }

    public VmLocalVolumeCacheState state;
    public void setState(VmLocalVolumeCacheState state) {
        this.state = state;
    }
    public VmLocalVolumeCacheState getState() {
        return this.state;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
