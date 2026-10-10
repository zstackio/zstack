package org.zstack.kvm.memory;

/** Public API inventory. Nullable samples represent unknown, never a fabricated zero. */
public class MemoryStateInventory {
    public static MemoryStateInventory __example__() {
        MemoryStateInventory inventory = new MemoryStateInventory();
        inventory.setHostUuid("1234567890abcdef1234567890abcdef");
        inventory.setBootId("1e73a145-2662-4911-9c4f-b75a7d91a88e");
        inventory.setFeatureState("Active");
        inventory.setFeatureStateSource("KSM_NATIVE");
        inventory.setDrift(false);
        inventory.setDataAgeSeconds(2L);
        // KSM runtime is confirmed, but this example has no savings sample.
        inventory.setQuality("Unknown");
        inventory.setMetrics(java.util.Collections.<String, Object>emptyMap());
        inventory.setControlOperationUuid("abcdef0123456789abcdef0123456789");
        inventory.setDesiredRevision(7L);
        inventory.setAppliedRevision(7L);
        inventory.setStatus("Succeeded");
        inventory.setState("{\"managed\":true,\"ksmOnly\":true,\"bootId\":\"" + inventory.getBootId()
                + "\",\"actual\":{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,\"pagesToScan\":1250,\"sleepMillis\":10}}}");
        inventory.setLastSampleTime(1791583200000L);
        inventory.setAppliedPolicy("{\"ksm\":{\"enabled\":true}}");
        inventory.setAppliedPolicyHash("3045708b6c3b9af13e9d314374300f24aec538669a8edcb5a509389cd0abe0e4");
        return inventory;
    }

    private String schemaVersion = "memory-state-v1";
    private String bootId;
    private String featureState;
    private String featureStateSource;
    private Boolean drift;
    private Long dataAgeSeconds;
    private String quality;
    private java.util.Map<String, Object> metrics;
    private java.util.List<String> allowedActions = java.util.Collections.singletonList("query");
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String value) { schemaVersion = value; }
    public String getBootId() { return bootId; }
    public void setBootId(String value) { bootId = value; }
    public String getFeatureState() { return featureState; }
    public void setFeatureState(String value) { featureState = value; }
    public String getFeatureStateSource() { return featureStateSource; }
    public void setFeatureStateSource(String value) { featureStateSource = value; }
    public Boolean getDrift() { return drift; }
    public void setDrift(Boolean value) { drift = value; }
    public Long getDataAgeSeconds() { return dataAgeSeconds; }
    public void setDataAgeSeconds(Long value) { dataAgeSeconds = value; }
    public String getQuality() { return quality; }
    public void setQuality(String value) { quality = value; }
    public java.util.Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(java.util.Map<String, Object> value) { metrics = value; }
    public java.util.List<String> getAllowedActions() { return allowedActions; }
    public void setAllowedActions(java.util.List<String> value) { allowedActions = value; }
    private String controlOperationUuid;
    public String getControlOperationUuid() { return controlOperationUuid; }
    public void setControlOperationUuid(String value) { controlOperationUuid = value; }
    public Long getSampleTime() { return lastSampleTime; }
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    private long desiredRevision;
    public long getDesiredRevision() { return desiredRevision; }
    public void setDesiredRevision(long value) { desiredRevision = value; }

    private Long appliedRevision;
    public Long getAppliedRevision() { return appliedRevision; }
    public void setAppliedRevision(Long value) { appliedRevision = value; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    private String capabilities;
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String value) { capabilities = value; }

    private String state;
    public String getState() { return state; }
    public void setState(String value) { state = value; }

    private Long lastSampleTime;
    public Long getLastSampleTime() { return lastSampleTime; }
    public void setLastSampleTime(Long value) { lastSampleTime = value; }

    private String activeTaskUuid;
    public String getActiveTaskUuid() { return activeTaskUuid; }
    public void setActiveTaskUuid(String value) { activeTaskUuid = value; }

    private String appliedPolicyHash;
    public String getAppliedPolicyHash() { return appliedPolicyHash; }
    public void setAppliedPolicyHash(String value) { appliedPolicyHash = value; }

    private String appliedPolicy;
    public String getAppliedPolicy() { return appliedPolicy; }
    public void setAppliedPolicy(String value) { appliedPolicy = value; }

    private String policyTargetHash;
    public String getPolicyTargetHash() { return policyTargetHash; }
    public void setPolicyTargetHash(String value) { policyTargetHash = value; }

    private String policyPlanHash;
    public String getPolicyPlanHash() { return policyPlanHash; }
    public void setPolicyPlanHash(String value) { policyPlanHash = value; }

    private String policyPlanStatus;
    public String getPolicyPlanStatus() { return policyPlanStatus; }
    public void setPolicyPlanStatus(String value) { policyPlanStatus = value; }

    private String policyBlockedFields;
    public String getPolicyBlockedFields() { return policyBlockedFields; }
    public void setPolicyBlockedFields(String value) { policyBlockedFields = value; }

}
