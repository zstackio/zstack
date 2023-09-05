package org.zstack.sdk;

import org.zstack.sdk.DpmOperationStatus;
import org.zstack.sdk.DpmOperation;

public class ClusterDpmRecordsInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String dpmUuid;
    public void setDpmUuid(java.lang.String dpmUuid) {
        this.dpmUuid = dpmUuid;
    }
    public java.lang.String getDpmUuid() {
        return this.dpmUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String hostName;
    public void setHostName(java.lang.String hostName) {
        this.hostName = hostName;
    }
    public java.lang.String getHostName() {
        return this.hostName;
    }

    public java.lang.Double cpuThreshold;
    public void setCpuThreshold(java.lang.Double cpuThreshold) {
        this.cpuThreshold = cpuThreshold;
    }
    public java.lang.Double getCpuThreshold() {
        return this.cpuThreshold;
    }

    public java.lang.Double memoryThreshold;
    public void setMemoryThreshold(java.lang.Double memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }
    public java.lang.Double getMemoryThreshold() {
        return this.memoryThreshold;
    }

    public DpmOperationStatus status;
    public void setStatus(DpmOperationStatus status) {
        this.status = status;
    }
    public DpmOperationStatus getStatus() {
        return this.status;
    }

    public DpmOperation operation;
    public void setOperation(DpmOperation operation) {
        this.operation = operation;
    }
    public DpmOperation getOperation() {
        return this.operation;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

    public java.lang.String errorDetail;
    public void setErrorDetail(java.lang.String errorDetail) {
        this.errorDetail = errorDetail;
    }
    public java.lang.String getErrorDetail() {
        return this.errorDetail;
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
