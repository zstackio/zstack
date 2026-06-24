package org.zstack.sdk;

import org.zstack.sdk.AiHostCacheStorageStatus;

public class AiHostCacheStorageInventory  {

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

    public java.lang.String sourceRoot;
    public void setSourceRoot(java.lang.String sourceRoot) {
        this.sourceRoot = sourceRoot;
    }
    public java.lang.String getSourceRoot() {
        return this.sourceRoot;
    }

    public java.lang.Long physicalTotalBytes;
    public void setPhysicalTotalBytes(java.lang.Long physicalTotalBytes) {
        this.physicalTotalBytes = physicalTotalBytes;
    }
    public java.lang.Long getPhysicalTotalBytes() {
        return this.physicalTotalBytes;
    }

    public java.lang.Long physicalAvailableBytes;
    public void setPhysicalAvailableBytes(java.lang.Long physicalAvailableBytes) {
        this.physicalAvailableBytes = physicalAvailableBytes;
    }
    public java.lang.Long getPhysicalAvailableBytes() {
        return this.physicalAvailableBytes;
    }

    public java.lang.Long policyUsedBytes;
    public void setPolicyUsedBytes(java.lang.Long policyUsedBytes) {
        this.policyUsedBytes = policyUsedBytes;
    }
    public java.lang.Long getPolicyUsedBytes() {
        return this.policyUsedBytes;
    }

    public java.lang.Long unmanagedUsedBytesEstimate;
    public void setUnmanagedUsedBytesEstimate(java.lang.Long unmanagedUsedBytesEstimate) {
        this.unmanagedUsedBytesEstimate = unmanagedUsedBytesEstimate;
    }
    public java.lang.Long getUnmanagedUsedBytesEstimate() {
        return this.unmanagedUsedBytesEstimate;
    }

    public java.lang.Long policyReservedBytes;
    public void setPolicyReservedBytes(java.lang.Long policyReservedBytes) {
        this.policyReservedBytes = policyReservedBytes;
    }
    public java.lang.Long getPolicyReservedBytes() {
        return this.policyReservedBytes;
    }

    public java.lang.Long policyMaxSizeBytes;
    public void setPolicyMaxSizeBytes(java.lang.Long policyMaxSizeBytes) {
        this.policyMaxSizeBytes = policyMaxSizeBytes;
    }
    public java.lang.Long getPolicyMaxSizeBytes() {
        return this.policyMaxSizeBytes;
    }

    public java.lang.Long effectiveAvailableBytes;
    public void setEffectiveAvailableBytes(java.lang.Long effectiveAvailableBytes) {
        this.effectiveAvailableBytes = effectiveAvailableBytes;
    }
    public java.lang.Long getEffectiveAvailableBytes() {
        return this.effectiveAvailableBytes;
    }

    public java.lang.Long highWatermarkBytes;
    public void setHighWatermarkBytes(java.lang.Long highWatermarkBytes) {
        this.highWatermarkBytes = highWatermarkBytes;
    }
    public java.lang.Long getHighWatermarkBytes() {
        return this.highWatermarkBytes;
    }

    public java.lang.Long lowWatermarkBytes;
    public void setLowWatermarkBytes(java.lang.Long lowWatermarkBytes) {
        this.lowWatermarkBytes = lowWatermarkBytes;
    }
    public java.lang.Long getLowWatermarkBytes() {
        return this.lowWatermarkBytes;
    }

    public AiHostCacheStorageStatus status;
    public void setStatus(AiHostCacheStorageStatus status) {
        this.status = status;
    }
    public AiHostCacheStorageStatus getStatus() {
        return this.status;
    }

    public java.lang.String statusReason;
    public void setStatusReason(java.lang.String statusReason) {
        this.statusReason = statusReason;
    }
    public java.lang.String getStatusReason() {
        return this.statusReason;
    }

    public java.sql.Timestamp lastSyncDate;
    public void setLastSyncDate(java.sql.Timestamp lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }
    public java.sql.Timestamp getLastSyncDate() {
        return this.lastSyncDate;
    }

}
