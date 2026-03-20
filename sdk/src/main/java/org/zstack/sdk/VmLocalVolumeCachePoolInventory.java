package org.zstack.sdk;

import org.zstack.sdk.VmLocalVolumeCachePoolState;
import org.zstack.sdk.VmLocalVolumeCachePoolStatus;

public class VmLocalVolumeCachePoolInventory  {

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

    public java.util.LinkedHashMap metadata;
    public void setMetadata(java.util.LinkedHashMap metadata) {
        this.metadata = metadata;
    }
    public java.util.LinkedHashMap getMetadata() {
        return this.metadata;
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

    public long allocated;
    public void setAllocated(long allocated) {
        this.allocated = allocated;
    }
    public long getAllocated() {
        return this.allocated;
    }

    public long dirty;
    public void setDirty(long dirty) {
        this.dirty = dirty;
    }
    public long getDirty() {
        return this.dirty;
    }

    public VmLocalVolumeCachePoolState state;
    public void setState(VmLocalVolumeCachePoolState state) {
        this.state = state;
    }
    public VmLocalVolumeCachePoolState getState() {
        return this.state;
    }

    public VmLocalVolumeCachePoolStatus status;
    public void setStatus(VmLocalVolumeCachePoolStatus status) {
        this.status = status;
    }
    public VmLocalVolumeCachePoolStatus getStatus() {
        return this.status;
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
