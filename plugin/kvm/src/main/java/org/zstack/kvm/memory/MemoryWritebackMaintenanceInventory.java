package org.zstack.kvm.memory;

import java.util.Map;

/** Public maintenance receipt projection; internal/unrecognized receipt fields are not forwarded. */
public class MemoryWritebackMaintenanceInventory {
    private Long schema;
    private String requestId;
    /** @deprecated r12 UI compatibility alias; use requestId. */
    @Deprecated private String request_id;
    private String candidateId;
    private String requestFingerprint;
    private String stage;
    private String reason;
    private String startedAt;
    private String updatedAt;
    /** @deprecated r12 UI compatibility alias; use updatedAt. */
    @Deprecated private String updated_at;
    private String hostBootId;
    private String poolGeneration;
    private MemoryWritebackMaintenanceTargetIdentityInventory targetIdentity;
    private Boolean resetAttempted;
    private String archive;
    private String backendResourceUuid;

    static MemoryWritebackMaintenanceInventory fromMap(Map<?, ?> source) {
        if (source == null) {
            return null;
        }
        MemoryWritebackMaintenanceInventory result = new MemoryWritebackMaintenanceInventory();
        result.schema = MemoryWritebackBackendInventory.exactLong(source.get("schema"));
        result.setRequestId(MemoryWritebackBackendInventory.string(source.get("request_id")));
        result.candidateId = MemoryWritebackBackendInventory.string(source.get("candidate_id"));
        result.requestFingerprint = MemoryWritebackBackendInventory.string(source.get("request_fingerprint"));
        result.stage = MemoryWritebackBackendInventory.string(source.get("stage"));
        result.reason = MemoryWritebackBackendInventory.string(source.get("reason"));
        result.startedAt = MemoryWritebackBackendInventory.string(source.get("started_at"));
        result.setUpdatedAt(MemoryWritebackBackendInventory.string(source.get("updated_at")));
        result.hostBootId = MemoryWritebackBackendInventory.string(source.get("host_boot_id"));
        result.poolGeneration = MemoryWritebackBackendInventory.string(source.get("pool_generation"));
        result.targetIdentity = MemoryWritebackMaintenanceTargetIdentityInventory.fromMap(
                MemoryWritebackBackendInventory.map(source.get("target_identity")));
        result.resetAttempted = MemoryWritebackBackendInventory.bool(source.get("reset_attempted"));
        result.archive = MemoryWritebackBackendInventory.string(source.get("archive"));
        result.backendResourceUuid = MemoryWritebackBackendInventory.string(source.get("backend_resource_uuid"));
        return result;
    }

    public Long getSchema() { return schema; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { requestId = value; request_id = value; }
    /** @deprecated r12 UI alias; use requestId. */
    @Deprecated public String getRequest_id() { return request_id; }
    @Deprecated public void setRequest_id(String value) { setRequestId(value); }
    public String getCandidateId() { return candidateId; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public String getStage() { return stage; }
    public String getReason() { return reason; }
    public String getStartedAt() { return startedAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String value) { updatedAt = value; updated_at = value; }
    /** @deprecated r12 UI alias; use updatedAt. */
    @Deprecated public String getUpdated_at() { return updated_at; }
    @Deprecated public void setUpdated_at(String value) { setUpdatedAt(value); }
    public String getHostBootId() { return hostBootId; }
    public String getPoolGeneration() { return poolGeneration; }
    public MemoryWritebackMaintenanceTargetIdentityInventory getTargetIdentity() { return targetIdentity; }
    public Boolean getResetAttempted() { return resetAttempted; }
    public String getArchive() { return archive; }
    public String getBackendResourceUuid() { return backendResourceUuid; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getCandidate_id() { return candidateId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getRequest_fingerprint() { return requestFingerprint; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getStarted_at() { return startedAt; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getHost_boot_id() { return hostBootId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getPool_generation() { return poolGeneration; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public MemoryWritebackMaintenanceTargetIdentityInventory getTarget_identity() { return targetIdentity; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Boolean getReset_attempted() { return resetAttempted; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getBackend_resource_uuid() { return backendResourceUuid; }
}
