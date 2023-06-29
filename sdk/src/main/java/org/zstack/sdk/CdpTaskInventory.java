package org.zstack.sdk;

import org.zstack.sdk.CdpTaskStatus;
import org.zstack.sdk.CdpTaskState;
import org.zstack.sdk.CdpTaskType;
import org.zstack.sdk.CdpTaskScene;

public class CdpTaskInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
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

    public java.lang.String policyUuid;
    public void setPolicyUuid(java.lang.String policyUuid) {
        this.policyUuid = policyUuid;
    }
    public java.lang.String getPolicyUuid() {
        return this.policyUuid;
    }

    public java.lang.String backupStorageUuid;
    public void setBackupStorageUuid(java.lang.String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
    public java.lang.String getBackupStorageUuid() {
        return this.backupStorageUuid;
    }

    public CdpTaskStatus status;
    public void setStatus(CdpTaskStatus status) {
        this.status = status;
    }
    public CdpTaskStatus getStatus() {
        return this.status;
    }

    public CdpTaskState state;
    public void setState(CdpTaskState state) {
        this.state = state;
    }
    public CdpTaskState getState() {
        return this.state;
    }

    public CdpTaskType taskType;
    public void setTaskType(CdpTaskType taskType) {
        this.taskType = taskType;
    }
    public CdpTaskType getTaskType() {
        return this.taskType;
    }

    public long backupBandwidth;
    public void setBackupBandwidth(long backupBandwidth) {
        this.backupBandwidth = backupBandwidth;
    }
    public long getBackupBandwidth() {
        return this.backupBandwidth;
    }

    public long maxCapacity;
    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    public long getMaxCapacity() {
        return this.maxCapacity;
    }

    public long usedCapacity;
    public void setUsedCapacity(long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }
    public long getUsedCapacity() {
        return this.usedCapacity;
    }

    public long maxLatency;
    public void setMaxLatency(long maxLatency) {
        this.maxLatency = maxLatency;
    }
    public long getMaxLatency() {
        return this.maxLatency;
    }

    public long lastLatency;
    public void setLastLatency(long lastLatency) {
        this.lastLatency = lastLatency;
    }
    public long getLastLatency() {
        return this.lastLatency;
    }

    public CdpTaskScene scene;
    public void setScene(CdpTaskScene scene) {
        this.scene = scene;
    }
    public CdpTaskScene getScene() {
        return this.scene;
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

    public java.util.List resourceRefs;
    public void setResourceRefs(java.util.List resourceRefs) {
        this.resourceRefs = resourceRefs;
    }
    public java.util.List getResourceRefs() {
        return this.resourceRefs;
    }

}
