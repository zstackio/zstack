package org.zstack.kvm.memory;

/** Fixed projection of per-VM accounting metrics. Null means unavailable; zero remains an observed zero. */
public class MemoryVmAccountingMetricsInventory {
    private Long compressedPages;
    private Long incompressiblePages;
    private Long zeroPages;
    private Long nonzeroSamePages;
    private Long backendCommittedPages;
    /** @deprecated r12 UI field alias; use ramPayloadBytes. */
    @Deprecated private Long ram_payload_bytes;
    private Long ramPayloadBytes;
    /** @deprecated r12 UI field alias; use ramOriginalBytes. */
    @Deprecated private Long ram_original_bytes;
    private Long ramOriginalBytes;
    /** @deprecated r12 UI field alias; use backendCommittedOriginalBytes. */
    @Deprecated private Long backend_committed_original_bytes;
    private Long backendCommittedOriginalBytes;
    private Long ramLogicalMinusPayloadBytes;
    private Double ramLogicalToPayloadRatio;
    private String ratioState;

    public static MemoryVmAccountingMetricsInventory __example__() {
        MemoryVmAccountingMetricsInventory metrics = new MemoryVmAccountingMetricsInventory();
        metrics.compressedPages = 20137L;
        metrics.incompressiblePages = 3005L;
        metrics.zeroPages = 2760L;
        metrics.nonzeroSamePages = 162L;
        metrics.backendCommittedPages = 0L;
        metrics.setRamPayloadBytes(38014641L);
        metrics.setRamOriginalBytes(106758144L);
        metrics.setBackendCommittedOriginalBytes(0L);
        metrics.ramLogicalMinusPayloadBytes = 68743503L;
        metrics.ramLogicalToPayloadRatio = 2.8083428171793074d;
        metrics.ratioState = "finite";
        return metrics;
    }

    public Long getCompressedPages() { return compressedPages; }
    public void setCompressedPages(Long value) { compressedPages = value; }
    public Long getIncompressiblePages() { return incompressiblePages; }
    public void setIncompressiblePages(Long value) { incompressiblePages = value; }
    public Long getZeroPages() { return zeroPages; }
    public void setZeroPages(Long value) { zeroPages = value; }
    public Long getNonzeroSamePages() { return nonzeroSamePages; }
    public void setNonzeroSamePages(Long value) { nonzeroSamePages = value; }
    public Long getBackendCommittedPages() { return backendCommittedPages; }
    public void setBackendCommittedPages(Long value) { backendCommittedPages = value; }
    public Long getRamPayloadBytes() { return ramPayloadBytes; }
    public void setRamPayloadBytes(Long value) { ramPayloadBytes = value; ram_payload_bytes = value; }
    /** @deprecated r12 UI alias; use ramPayloadBytes. */
    @Deprecated public Long getRam_payload_bytes() { return ram_payload_bytes; }
    @Deprecated public void setRam_payload_bytes(Long value) { setRamPayloadBytes(value); }
    public Long getRamOriginalBytes() { return ramOriginalBytes; }
    public void setRamOriginalBytes(Long value) { ramOriginalBytes = value; ram_original_bytes = value; }
    /** @deprecated r12 UI alias; use ramOriginalBytes. */
    @Deprecated public Long getRam_original_bytes() { return ram_original_bytes; }
    @Deprecated public void setRam_original_bytes(Long value) { setRamOriginalBytes(value); }
    public Long getBackendCommittedOriginalBytes() { return backendCommittedOriginalBytes; }
    public void setBackendCommittedOriginalBytes(Long value) { backendCommittedOriginalBytes = value; backend_committed_original_bytes = value; }
    /** @deprecated r12 UI alias; use backendCommittedOriginalBytes. */
    @Deprecated public Long getBackend_committed_original_bytes() { return backend_committed_original_bytes; }
    @Deprecated public void setBackend_committed_original_bytes(Long value) { setBackendCommittedOriginalBytes(value); }
    public Long getRamLogicalMinusPayloadBytes() { return ramLogicalMinusPayloadBytes; }
    public void setRamLogicalMinusPayloadBytes(Long value) { ramLogicalMinusPayloadBytes = value; }
    public Double getRamLogicalToPayloadRatio() { return ramLogicalToPayloadRatio; }
    public void setRamLogicalToPayloadRatio(Double value) { ramLogicalToPayloadRatio = value; }
    public String getRatioState() { return ratioState; }
    public void setRatioState(String value) { ratioState = value; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getCompressed_pages() { return compressedPages; }
    @Deprecated public void setCompressed_pages(Long value) { setCompressedPages(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getIncompressible_pages() { return incompressiblePages; }
    @Deprecated public void setIncompressible_pages(Long value) { setIncompressiblePages(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getZero_pages() { return zeroPages; }
    @Deprecated public void setZero_pages(Long value) { setZeroPages(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getNonzero_same_pages() { return nonzeroSamePages; }
    @Deprecated public void setNonzero_same_pages(Long value) { setNonzeroSamePages(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getBackend_committed_pages() { return backendCommittedPages; }
    @Deprecated public void setBackend_committed_pages(Long value) { setBackendCommittedPages(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getRam_logical_minus_payload_bytes() { return ramLogicalMinusPayloadBytes; }
    @Deprecated public void setRam_logical_minus_payload_bytes(Long value) { setRamLogicalMinusPayloadBytes(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Double getRam_logical_to_payload_ratio() { return ramLogicalToPayloadRatio; }
    @Deprecated public void setRam_logical_to_payload_ratio(Double value) { setRamLogicalToPayloadRatio(value); }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getRatio_state() { return ratioState; }
    @Deprecated public void setRatio_state(String value) { setRatioState(value); }
}
