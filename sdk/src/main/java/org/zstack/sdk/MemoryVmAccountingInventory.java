package org.zstack.sdk;

import org.zstack.sdk.MemoryVmIdentityInventory;
import org.zstack.sdk.MemoryVmAccountingMetricsInventory;

public class MemoryVmAccountingInventory  {

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String vmUuid;
    public void setVmUuid(java.lang.String vmUuid) {
        this.vmUuid = vmUuid;
    }
    public java.lang.String getVmUuid() {
        return this.vmUuid;
    }

    public java.lang.String quality;
    public void setQuality(java.lang.String quality) {
        this.quality = quality;
    }
    public java.lang.String getQuality() {
        return this.quality;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

    public java.lang.String observedAt;
    public void setObservedAt(java.lang.String observedAt) {
        this.observedAt = observedAt;
    }
    public java.lang.String getObservedAt() {
        return this.observedAt;
    }

    public java.lang.String observed_at;
    public void setObserved_at(java.lang.String observed_at) {
        this.observed_at = observed_at;
    }
    public java.lang.String getObserved_at() {
        return this.observed_at;
    }

    public java.lang.String hostBootId;
    public void setHostBootId(java.lang.String hostBootId) {
        this.hostBootId = hostBootId;
    }
    public java.lang.String getHostBootId() {
        return this.hostBootId;
    }

    public java.lang.String device;
    public void setDevice(java.lang.String device) {
        this.device = device;
    }
    public java.lang.String getDevice() {
        return this.device;
    }

    public java.lang.String ownershipSemantics;
    public void setOwnershipSemantics(java.lang.String ownershipSemantics) {
        this.ownershipSemantics = ownershipSemantics;
    }
    public java.lang.String getOwnershipSemantics() {
        return this.ownershipSemantics;
    }

    public java.lang.Long displayTtlMillis;
    public void setDisplayTtlMillis(java.lang.Long displayTtlMillis) {
        this.displayTtlMillis = displayTtlMillis;
    }
    public java.lang.Long getDisplayTtlMillis() {
        return this.displayTtlMillis;
    }

    public java.lang.Integer schemaVersion;
    public void setSchemaVersion(java.lang.Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
    public java.lang.Integer getSchemaVersion() {
        return this.schemaVersion;
    }

    public java.lang.String poolGeneration;
    public void setPoolGeneration(java.lang.String poolGeneration) {
        this.poolGeneration = poolGeneration;
    }
    public java.lang.String getPoolGeneration() {
        return this.poolGeneration;
    }

    public java.lang.String sequence;
    public void setSequence(java.lang.String sequence) {
        this.sequence = sequence;
    }
    public java.lang.String getSequence() {
        return this.sequence;
    }

    public MemoryVmIdentityInventory identity;
    public void setIdentity(MemoryVmIdentityInventory identity) {
        this.identity = identity;
    }
    public MemoryVmIdentityInventory getIdentity() {
        return this.identity;
    }

    public MemoryVmAccountingMetricsInventory metrics;
    public void setMetrics(MemoryVmAccountingMetricsInventory metrics) {
        this.metrics = metrics;
    }
    public MemoryVmAccountingMetricsInventory getMetrics() {
        return this.metrics;
    }

}
