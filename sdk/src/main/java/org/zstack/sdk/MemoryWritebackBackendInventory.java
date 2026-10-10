package org.zstack.sdk;

import org.zstack.sdk.MemoryWritebackMaintenanceInventory;

public class MemoryWritebackBackendInventory  {

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String bootId;
    public void setBootId(java.lang.String bootId) {
        this.bootId = bootId;
    }
    public java.lang.String getBootId() {
        return this.bootId;
    }

    public java.lang.String observedAt;
    public void setObservedAt(java.lang.String observedAt) {
        this.observedAt = observedAt;
    }
    public java.lang.String getObservedAt() {
        return this.observedAt;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.util.List reasons;
    public void setReasons(java.util.List reasons) {
        this.reasons = reasons;
    }
    public java.util.List getReasons() {
        return this.reasons;
    }

    public java.lang.Boolean candidatesFieldPresent;
    public void setCandidatesFieldPresent(java.lang.Boolean candidatesFieldPresent) {
        this.candidatesFieldPresent = candidatesFieldPresent;
    }
    public java.lang.Boolean getCandidatesFieldPresent() {
        return this.candidatesFieldPresent;
    }

    public java.util.List candidates;
    public void setCandidates(java.util.List candidates) {
        this.candidates = candidates;
    }
    public java.util.List getCandidates() {
        return this.candidates;
    }

    public java.lang.String controlOperationUuid;
    public void setControlOperationUuid(java.lang.String controlOperationUuid) {
        this.controlOperationUuid = controlOperationUuid;
    }
    public java.lang.String getControlOperationUuid() {
        return this.controlOperationUuid;
    }

    public MemoryWritebackMaintenanceInventory maintenance;
    public void setMaintenance(MemoryWritebackMaintenanceInventory maintenance) {
        this.maintenance = maintenance;
    }
    public MemoryWritebackMaintenanceInventory getMaintenance() {
        return this.maintenance;
    }

    public java.lang.String poolGeneration;
    public void setPoolGeneration(java.lang.String poolGeneration) {
        this.poolGeneration = poolGeneration;
    }
    public java.lang.String getPoolGeneration() {
        return this.poolGeneration;
    }

    public java.lang.Boolean canPrepare;
    public void setCanPrepare(java.lang.Boolean canPrepare) {
        this.canPrepare = canPrepare;
    }
    public java.lang.Boolean getCanPrepare() {
        return this.canPrepare;
    }

    public java.util.List preparationReasons;
    public void setPreparationReasons(java.util.List preparationReasons) {
        this.preparationReasons = preparationReasons;
    }
    public java.util.List getPreparationReasons() {
        return this.preparationReasons;
    }

    public java.lang.Boolean canPrepareZramPool;
    public void setCanPrepareZramPool(java.lang.Boolean canPrepareZramPool) {
        this.canPrepareZramPool = canPrepareZramPool;
    }
    public java.lang.Boolean getCanPrepareZramPool() {
        return this.canPrepareZramPool;
    }

    public java.util.List zramPoolPreparationReasons;
    public void setZramPoolPreparationReasons(java.util.List zramPoolPreparationReasons) {
        this.zramPoolPreparationReasons = zramPoolPreparationReasons;
    }
    public java.util.List getZramPoolPreparationReasons() {
        return this.zramPoolPreparationReasons;
    }

    public java.lang.String zramPoolPreparationQuality;
    public void setZramPoolPreparationQuality(java.lang.String zramPoolPreparationQuality) {
        this.zramPoolPreparationQuality = zramPoolPreparationQuality;
    }
    public java.lang.String getZramPoolPreparationQuality() {
        return this.zramPoolPreparationQuality;
    }

    public java.lang.Long zramPoolPreparationObservedAt;
    public void setZramPoolPreparationObservedAt(java.lang.Long zramPoolPreparationObservedAt) {
        this.zramPoolPreparationObservedAt = zramPoolPreparationObservedAt;
    }
    public java.lang.Long getZramPoolPreparationObservedAt() {
        return this.zramPoolPreparationObservedAt;
    }

}
