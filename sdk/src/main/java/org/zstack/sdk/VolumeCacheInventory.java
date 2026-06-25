package org.zstack.sdk;

import org.zstack.sdk.VolumeCacheMode;
import org.zstack.sdk.VolumeCacheStatus;

public class VolumeCacheInventory  {

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

    public VolumeCacheMode cacheMode;
    public void setCacheMode(VolumeCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }
    public VolumeCacheMode getCacheMode() {
        return this.cacheMode;
    }

    public VolumeCacheStatus status;
    public void setStatus(VolumeCacheStatus status) {
        this.status = status;
    }
    public VolumeCacheStatus getStatus() {
        return this.status;
    }

    public java.lang.Long virtualSize;
    public void setVirtualSize(java.lang.Long virtualSize) {
        this.virtualSize = virtualSize;
    }
    public java.lang.Long getVirtualSize() {
        return this.virtualSize;
    }

    public java.lang.Long actualSize;
    public void setActualSize(java.lang.Long actualSize) {
        this.actualSize = actualSize;
    }
    public java.lang.Long getActualSize() {
        return this.actualSize;
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
