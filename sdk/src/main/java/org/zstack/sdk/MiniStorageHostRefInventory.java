package org.zstack.sdk;

import org.zstack.sdk.PrimaryStorageHostStatus;

public class MiniStorageHostRefInventory  {

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.Long totalCapacity;
    public void setTotalCapacity(java.lang.Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public java.lang.Long getTotalCapacity() {
        return this.totalCapacity;
    }

    public java.lang.Long availableCapacity;
    public void setAvailableCapacity(java.lang.Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public java.lang.Long getAvailableCapacity() {
        return this.availableCapacity;
    }

    public java.lang.Long totalPhysicalCapacity;
    public void setTotalPhysicalCapacity(java.lang.Long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }
    public java.lang.Long getTotalPhysicalCapacity() {
        return this.totalPhysicalCapacity;
    }

    public java.lang.Long availablePhysicalCapacity;
    public void setAvailablePhysicalCapacity(java.lang.Long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }
    public java.lang.Long getAvailablePhysicalCapacity() {
        return this.availablePhysicalCapacity;
    }

    public PrimaryStorageHostStatus status;
    public void setStatus(PrimaryStorageHostStatus status) {
        this.status = status;
    }
    public PrimaryStorageHostStatus getStatus() {
        return this.status;
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
