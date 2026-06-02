package org.zstack.sdk;

import org.zstack.sdk.HostCacheStoreState;
import org.zstack.sdk.HostCacheStoreStatus;

public class HostCacheStoreInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String mountPoint;
    public void setMountPoint(java.lang.String mountPoint) {
        this.mountPoint = mountPoint;
    }
    public java.lang.String getMountPoint() {
        return this.mountPoint;
    }

    public long totalCapacity;
    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public long getTotalCapacity() {
        return this.totalCapacity;
    }

    public long availableCapacity;
    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public long getAvailableCapacity() {
        return this.availableCapacity;
    }

    public long totalPhysicalCapacity;
    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }
    public long getTotalPhysicalCapacity() {
        return this.totalPhysicalCapacity;
    }

    public long availablePhysicalCapacity;
    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }
    public long getAvailablePhysicalCapacity() {
        return this.availablePhysicalCapacity;
    }

    public long systemUsedCapacity;
    public void setSystemUsedCapacity(long systemUsedCapacity) {
        this.systemUsedCapacity = systemUsedCapacity;
    }
    public long getSystemUsedCapacity() {
        return this.systemUsedCapacity;
    }

    public HostCacheStoreState state;
    public void setState(HostCacheStoreState state) {
        this.state = state;
    }
    public HostCacheStoreState getState() {
        return this.state;
    }

    public HostCacheStoreStatus status;
    public void setStatus(HostCacheStoreStatus status) {
        this.status = status;
    }
    public HostCacheStoreStatus getStatus() {
        return this.status;
    }

    public java.util.List devices;
    public void setDevices(java.util.List devices) {
        this.devices = devices;
    }
    public java.util.List getDevices() {
        return this.devices;
    }

    public java.util.Set caches;
    public void setCaches(java.util.Set caches) {
        this.caches = caches;
    }
    public java.util.Set getCaches() {
        return this.caches;
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
