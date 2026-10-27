package org.zstack.kvm.memory;

public class MemoryStateInventory {
    private String bootId;
    private String featureState;
    private String quality;
    private java.util.Map<String, Object> metrics;
    private java.util.List<String> allowedActions = java.util.Collections.singletonList("query");
    public String getBootId() { return bootId; }
    public void setBootId(String value) { bootId = value; }
    public String getFeatureState() { return featureState; }
    public void setFeatureState(String value) { featureState = value; }
    public String getQuality() { return quality; }
    public void setQuality(String value) { quality = value; }
    public java.util.Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(java.util.Map<String, Object> value) { metrics = value; }
    public java.util.List<String> getAllowedActions() { return allowedActions; }
    public void setAllowedActions(java.util.List<String> value) { allowedActions = value; }
    /** The exact request recoverUncertain accepts now; present only with that allowed action. */
    private java.util.Map<String, String> uncertainRecovery;
    public java.util.Map<String, String> getUncertainRecovery() { return uncertainRecovery; }
    public void setUncertainRecovery(java.util.Map<String, String> value) { uncertainRecovery = value; }
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
