package org.zstack.sdk;

import org.zstack.sdk.SlbVmInstanceConfigTaskStatus;

public class SlbVmInstanceConfigTaskInventory  {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public long configVersion;
    public void setConfigVersion(long configVersion) {
        this.configVersion = configVersion;
    }
    public long getConfigVersion() {
        return this.configVersion;
    }

    public java.lang.String taskName;
    public void setTaskName(java.lang.String taskName) {
        this.taskName = taskName;
    }
    public java.lang.String getTaskName() {
        return this.taskName;
    }

    public java.lang.String taskData;
    public void setTaskData(java.lang.String taskData) {
        this.taskData = taskData;
    }
    public java.lang.String getTaskData() {
        return this.taskData;
    }

    public long retryNumber;
    public void setRetryNumber(long retryNumber) {
        this.retryNumber = retryNumber;
    }
    public long getRetryNumber() {
        return this.retryNumber;
    }

    public java.lang.String lastFailedReason;
    public void setLastFailedReason(java.lang.String lastFailedReason) {
        this.lastFailedReason = lastFailedReason;
    }
    public java.lang.String getLastFailedReason() {
        return this.lastFailedReason;
    }

    public SlbVmInstanceConfigTaskStatus status;
    public void setStatus(SlbVmInstanceConfigTaskStatus status) {
        this.status = status;
    }
    public SlbVmInstanceConfigTaskStatus getStatus() {
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
