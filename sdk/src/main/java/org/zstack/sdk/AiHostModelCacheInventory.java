package org.zstack.sdk;

import org.zstack.sdk.AiHostModelCacheStatus;
import org.zstack.sdk.AiHostModelCacheFailurePhase;
import org.zstack.sdk.AiHostModelCacheFailureCode;

public class AiHostModelCacheInventory  {

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

    public java.lang.String modelCenterUuid;
    public void setModelCenterUuid(java.lang.String modelCenterUuid) {
        this.modelCenterUuid = modelCenterUuid;
    }
    public java.lang.String getModelCenterUuid() {
        return this.modelCenterUuid;
    }

    public java.lang.String modelUuid;
    public void setModelUuid(java.lang.String modelUuid) {
        this.modelUuid = modelUuid;
    }
    public java.lang.String getModelUuid() {
        return this.modelUuid;
    }

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public java.lang.String primaryStorageName;
    public void setPrimaryStorageName(java.lang.String primaryStorageName) {
        this.primaryStorageName = primaryStorageName;
    }
    public java.lang.String getPrimaryStorageName() {
        return this.primaryStorageName;
    }

    public java.lang.String sourceRoot;
    public void setSourceRoot(java.lang.String sourceRoot) {
        this.sourceRoot = sourceRoot;
    }
    public java.lang.String getSourceRoot() {
        return this.sourceRoot;
    }

    public java.lang.String sourcePath;
    public void setSourcePath(java.lang.String sourcePath) {
        this.sourcePath = sourcePath;
    }
    public java.lang.String getSourcePath() {
        return this.sourcePath;
    }

    public java.lang.Long sizeBytes;
    public void setSizeBytes(java.lang.Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    public java.lang.Long getSizeBytes() {
        return this.sizeBytes;
    }

    public java.lang.Long sourceMtime;
    public void setSourceMtime(java.lang.Long sourceMtime) {
        this.sourceMtime = sourceMtime;
    }
    public java.lang.Long getSourceMtime() {
        return this.sourceMtime;
    }

    public java.lang.String checksum;
    public void setChecksum(java.lang.String checksum) {
        this.checksum = checksum;
    }
    public java.lang.String getChecksum() {
        return this.checksum;
    }

    public java.lang.String contentVersion;
    public void setContentVersion(java.lang.String contentVersion) {
        this.contentVersion = contentVersion;
    }
    public java.lang.String getContentVersion() {
        return this.contentVersion;
    }

    public java.lang.String identityHash;
    public void setIdentityHash(java.lang.String identityHash) {
        this.identityHash = identityHash;
    }
    public java.lang.String getIdentityHash() {
        return this.identityHash;
    }

    public AiHostModelCacheStatus status;
    public void setStatus(AiHostModelCacheStatus status) {
        this.status = status;
    }
    public AiHostModelCacheStatus getStatus() {
        return this.status;
    }

    public long desiredRefCount;
    public void setDesiredRefCount(long desiredRefCount) {
        this.desiredRefCount = desiredRefCount;
    }
    public long getDesiredRefCount() {
        return this.desiredRefCount;
    }

    public long runningRefCount;
    public void setRunningRefCount(long runningRefCount) {
        this.runningRefCount = runningRefCount;
    }
    public long getRunningRefCount() {
        return this.runningRefCount;
    }

    public java.lang.String reservationUuid;
    public void setReservationUuid(java.lang.String reservationUuid) {
        this.reservationUuid = reservationUuid;
    }
    public java.lang.String getReservationUuid() {
        return this.reservationUuid;
    }

    public java.lang.Integer waiterCount;
    public void setWaiterCount(java.lang.Integer waiterCount) {
        this.waiterCount = waiterCount;
    }
    public java.lang.Integer getWaiterCount() {
        return this.waiterCount;
    }

    public java.sql.Timestamp lastAccessDate;
    public void setLastAccessDate(java.sql.Timestamp lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }
    public java.sql.Timestamp getLastAccessDate() {
        return this.lastAccessDate;
    }

    public java.sql.Timestamp lastSyncDate;
    public void setLastSyncDate(java.sql.Timestamp lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }
    public java.sql.Timestamp getLastSyncDate() {
        return this.lastSyncDate;
    }

    public AiHostModelCacheFailurePhase failurePhase;
    public void setFailurePhase(AiHostModelCacheFailurePhase failurePhase) {
        this.failurePhase = failurePhase;
    }
    public AiHostModelCacheFailurePhase getFailurePhase() {
        return this.failurePhase;
    }

    public AiHostModelCacheFailureCode failureCode;
    public void setFailureCode(AiHostModelCacheFailureCode failureCode) {
        this.failureCode = failureCode;
    }
    public AiHostModelCacheFailureCode getFailureCode() {
        return this.failureCode;
    }

    public java.lang.String failureMessage;
    public void setFailureMessage(java.lang.String failureMessage) {
        this.failureMessage = failureMessage;
    }
    public java.lang.String getFailureMessage() {
        return this.failureMessage;
    }

}
