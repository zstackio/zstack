package org.zstack.sdk;

import org.zstack.sdk.SharedBlockType;
import org.zstack.sdk.SharedBlockState;
import org.zstack.sdk.SharedBlockStatus;

public class SharedBlockInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String sharedBlockGroupUuid;
    public void setSharedBlockGroupUuid(java.lang.String sharedBlockGroupUuid) {
        this.sharedBlockGroupUuid = sharedBlockGroupUuid;
    }
    public java.lang.String getSharedBlockGroupUuid() {
        return this.sharedBlockGroupUuid;
    }

    public SharedBlockType type;
    public void setType(SharedBlockType type) {
        this.type = type;
    }
    public SharedBlockType getType() {
        return this.type;
    }

    public java.lang.String diskUuid;
    public void setDiskUuid(java.lang.String diskUuid) {
        this.diskUuid = diskUuid;
    }
    public java.lang.String getDiskUuid() {
        return this.diskUuid;
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

    public SharedBlockState state;
    public void setState(SharedBlockState state) {
        this.state = state;
    }
    public SharedBlockState getState() {
        return this.state;
    }

    public SharedBlockStatus status;
    public void setStatus(SharedBlockStatus status) {
        this.status = status;
    }
    public SharedBlockStatus getStatus() {
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

    public java.lang.Long totalCpuCapacity;
    public Long getTotalCpuCapacity() {
        return totalCpuCapacity;
    }
    public void setTotalCpuCapacity(Long totalCpuCapacity) {
        this.totalCpuCapacity = totalCpuCapacity;
    }

    public java.lang.Long availableCapacity;
    public Long getAvailableCapacity() {
        return availableCapacity;
    }
    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
}
