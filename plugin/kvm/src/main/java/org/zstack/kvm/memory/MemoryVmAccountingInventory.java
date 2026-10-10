package org.zstack.kvm.memory;

/** Fixed public projection for a single requested VM's ZRAM accounting sample. */
public class MemoryVmAccountingInventory {
    private String hostUuid;
    private String vmUuid;
    private String quality;
    private String reason;
    private String observedAt;
    /** @deprecated r12 UI wire-name compatibility; remove after the listed consumers migrate. */
    @Deprecated private String observed_at;
    private String hostBootId;
    private String device;
    private String ownershipSemantics;
    private Long displayTtlMillis;
    private Integer schemaVersion;
    private String poolGeneration;
    private String sequence;
    private MemoryVmIdentityInventory identity;
    private MemoryVmAccountingMetricsInventory metrics;

    public static MemoryVmAccountingInventory __example__() {
        MemoryVmAccountingInventory vm = new MemoryVmAccountingInventory();
        vm.hostUuid = "093d46206e694835b4a218449eb1bc7c";
        vm.vmUuid = "03d98313faa24670a0944b10da8d1ac2";
        vm.quality = "partial_unattributed_present";
        vm.observedAt = "2026-10-08T00:39:24.983155465+08:00";
        vm.observed_at = vm.observedAt;
        vm.hostBootId = "8c6ce488-e264-4c1d-bcd9-763196d10a3e";
        vm.device = "/dev/zram0";
        vm.ownershipSemantics = "source_page_memcg_at_successful_commit; KSM is charge ownership, not fair shared cost";
        vm.displayTtlMillis = 90000L;
        vm.schemaVersion = 2;
        vm.poolGeneration = "1051626769722038414";
        vm.sequence = "1177499";
        vm.identity = MemoryVmIdentityInventory.__example__();
        vm.metrics = MemoryVmAccountingMetricsInventory.__example__();
        return vm;
    }

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getVmUuid() { return vmUuid; }
    public void setVmUuid(String value) { vmUuid = value; }
    public String getQuality() { return quality; }
    public void setQuality(String value) { quality = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public String getObservedAt() { return observedAt; }
    public void setObservedAt(String value) { observedAt = value; observed_at = value; }
    /** @deprecated r12 UI compatibility alias; use observedAt. */
    @Deprecated public String getObserved_at() { return observed_at; }
    @Deprecated public void setObserved_at(String value) { observedAt = value; observed_at = value; }
    public String getHostBootId() { return hostBootId; }
    public void setHostBootId(String value) { hostBootId = value; }
    public String getDevice() { return device; }
    public void setDevice(String value) { device = value; }
    public String getOwnershipSemantics() { return ownershipSemantics; }
    public void setOwnershipSemantics(String value) { ownershipSemantics = value; }
    public Long getDisplayTtlMillis() { return displayTtlMillis; }
    public void setDisplayTtlMillis(Long value) { displayTtlMillis = value; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer value) { schemaVersion = value; }
    public String getPoolGeneration() { return poolGeneration; }
    public void setPoolGeneration(String value) { poolGeneration = value; }
    public String getSequence() { return sequence; }
    public void setSequence(String value) { sequence = value; }
    public MemoryVmIdentityInventory getIdentity() { return identity; }
    public void setIdentity(MemoryVmIdentityInventory value) { identity = value; }
    public MemoryVmAccountingMetricsInventory getMetrics() { return metrics; }
    public void setMetrics(MemoryVmAccountingMetricsInventory value) { metrics = value; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getHost_boot_id() { return hostBootId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getOwnership_semantics() { return ownershipSemantics; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Integer getSchema_version() { return schemaVersion; }
}
