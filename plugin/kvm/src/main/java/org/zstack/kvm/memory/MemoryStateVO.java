package org.zstack.kvm.memory;

import javax.persistence.*;

@Entity
@Table
public class MemoryStateVO {
    @Column(length = 64)
    private String appliedPolicyHash;
    public String getAppliedPolicyHash() { return appliedPolicyHash; }
    public void setAppliedPolicyHash(String value) { appliedPolicyHash = value; }

    @Column(columnDefinition = "MEDIUMTEXT")
    private String appliedPolicy;
    public String getAppliedPolicy() { return appliedPolicy; }
    public void setAppliedPolicy(String value) { appliedPolicy = value; }

    @Column(length = 64)
    private String policyTargetHash;
    public String getPolicyTargetHash() { return policyTargetHash; }
    public void setPolicyTargetHash(String value) { policyTargetHash = value; }

    @Column(length = 64)
    private String policyPlanHash;
    public String getPolicyPlanHash() { return policyPlanHash; }
    public void setPolicyPlanHash(String value) { policyPlanHash = value; }

    @Column(length = 16)
    private String policyPlanStatus;
    public String getPolicyPlanStatus() { return policyPlanStatus; }
    public void setPolicyPlanStatus(String value) { policyPlanStatus = value; }

    @Column(columnDefinition = "TEXT")
    private String policyBlockedFields;
    public String getPolicyBlockedFields() { return policyBlockedFields; }
    public void setPolicyBlockedFields(String value) { policyBlockedFields = value; }

    @Column(nullable = false)
    private boolean permitAuthorized;
    public boolean isPermitAuthorized() { return permitAuthorized; }
    public void setPermitAuthorized(boolean value) { permitAuthorized = value; }
    @Column
    private Long permitDeadline;
    public Long getPermitDeadline() { return permitDeadline; }
    public void setPermitDeadline(Long value) { permitDeadline = value; }
    @Column(length = 32)
    private String controlOperationUuid;
    public String getControlOperationUuid() { return controlOperationUuid; }
    public void setControlOperationUuid(String value) { controlOperationUuid = value; }
    @Id @Column(length = 32)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    @Column(nullable = false)
    private long desiredRevision;
    public long getDesiredRevision() { return desiredRevision; }
    public void setDesiredRevision(long value) { desiredRevision = value; }

    @Column
    private Long appliedRevision;
    public Long getAppliedRevision() { return appliedRevision; }
    public void setAppliedRevision(Long value) { appliedRevision = value; }

    @Column(nullable = false, length = 32)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(columnDefinition = "TEXT")
    private String capabilities;
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String value) { capabilities = value; }

    @Column(columnDefinition = "MEDIUMTEXT")
    private String state;
    public String getState() { return state; }
    public void setState(String value) { state = value; }

    @Column
    private Long lastSampleTime;
    public Long getLastSampleTime() { return lastSampleTime; }
    public void setLastSampleTime(Long value) { lastSampleTime = value; }

    @Column(length = 32)
    private String activeTaskUuid;
    public String getActiveTaskUuid() { return activeTaskUuid; }
    public void setActiveTaskUuid(String value) { activeTaskUuid = value; }

    @Column(nullable = false)
    private java.sql.Timestamp createDate;
    public java.sql.Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(java.sql.Timestamp value) { createDate = value; }

    @Column(nullable = false)
    private java.sql.Timestamp lastOpDate;
    public java.sql.Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(java.sql.Timestamp value) { lastOpDate = value; }

    @PrePersist
    private void onCreate() {
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        createDate = now;
        lastOpDate = now;
    }

    @PreUpdate
    private void onUpdate() { lastOpDate = new java.sql.Timestamp(System.currentTimeMillis()); }

    public MemoryStateInventory toInventory() {
        MemoryStateInventory result = new MemoryStateInventory();
        result.setHostUuid(hostUuid);
        result.setDesiredRevision(desiredRevision);
        result.setAppliedRevision(appliedRevision);
        result.setStatus(status);
        result.setReason(reason);
        result.setCapabilities(capabilities);
        result.setState(state);
        result.setLastSampleTime(lastSampleTime);
        result.setActiveTaskUuid(activeTaskUuid);
        result.setControlOperationUuid(controlOperationUuid);
        result.setAppliedPolicyHash(appliedPolicyHash);
        result.setAppliedPolicy(appliedPolicy);
        result.setPolicyTargetHash(policyTargetHash);
        result.setPolicyPlanHash(policyPlanHash);
        result.setPolicyPlanStatus(policyPlanStatus);
        result.setPolicyBlockedFields(policyBlockedFields);
        MemoryStatePresentation.populate(result, System.currentTimeMillis());
        return result;
    }
}
