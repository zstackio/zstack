package org.zstack.kvm.memory;

import javax.persistence.*;

@Entity
@Table
public class MemoryTaskVO {
    @Column(length = 32)
    private String reconcileOperationUuid;
    public String getReconcileOperationUuid() { return reconcileOperationUuid; }
    public void setReconcileOperationUuid(String value) { reconcileOperationUuid = value; }
    @Column(length = 128)
    private String expectedInstanceGeneration;
    public String getExpectedInstanceGeneration() { return expectedInstanceGeneration; }
    public void setExpectedInstanceGeneration(String value) { expectedInstanceGeneration = value; }
    @Column(length = 32)
    private String expectedControlOperationUuid;
    public String getExpectedControlOperationUuid() { return expectedControlOperationUuid; }
    public void setExpectedControlOperationUuid(String value) { expectedControlOperationUuid = value; }
    @Column(length = 32)
    private String issuedPermitOperationUuid;
    public String getIssuedPermitOperationUuid() { return issuedPermitOperationUuid; }
    public void setIssuedPermitOperationUuid(String value) { issuedPermitOperationUuid = value; }
    @Column
    private Long issuedPermitDeadline;
    public Long getIssuedPermitDeadline() { return issuedPermitDeadline; }
    public void setIssuedPermitDeadline(Long value) { issuedPermitDeadline = value; }
    @Id @Column(length = 32)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Column(length = 32)
    private String parentUuid;
    public String getParentUuid() { return parentUuid; }
    public void setParentUuid(String value) { parentUuid = value; }

    @Column(length = 32)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    @Column(nullable = false, length = 16)
    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    @Column(nullable = false, length = 64)
    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    @Column(nullable = false, length = 80)
    private String actorUuid;
    public String getActorUuid() { return actorUuid; }
    public void setActorUuid(String value) { actorUuid = value; }

    @Column(unique = true, length = 160)
    private String requestKey;
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }

    @Column(length = 64)
    private String requestHash;
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { requestHash = value; }

    /** Receipt for a target snapshot commit, independent of expiring staged rows. */
    @Column(length = 64)
    private String targetSnapshotHash;
    public String getTargetSnapshotHash() { return targetSnapshotHash; }
    public void setTargetSnapshotHash(String value) { targetSnapshotHash = value; }

    @Column(length = 64)
    private String policyPlanHash;
    public String getPolicyPlanHash() { return policyPlanHash; }
    public void setPolicyPlanHash(String value) { policyPlanHash = value; }

    @Column(nullable = false, length = 32)
    private String action;
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }

    @Column(nullable = false, length = 32)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(nullable = false)
    private long desiredRevision;
    public long getDesiredRevision() { return desiredRevision; }
    public void setDesiredRevision(long value) { desiredRevision = value; }

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String policy;
    public String getPolicy() { return policy; }
    public void setPolicy(String value) { policy = value; }

    @Column(length = 32)
    private String ownerManagementNodeUuid;
    public String getOwnerManagementNodeUuid() { return ownerManagementNodeUuid; }
    public void setOwnerManagementNodeUuid(String value) { ownerManagementNodeUuid = value; }

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

    public MemoryTaskInventory toInventory() {
        MemoryTaskInventory result = new MemoryTaskInventory();
        result.setUuid(uuid);
        result.setParentUuid(parentUuid);
        result.setHostUuid(hostUuid);
        result.setScope(scope);
        result.setResourceUuid(resourceUuid);
        result.setAction(action);
        result.setStatus(status);
        result.setReason(reason);
        result.setDesiredRevision(desiredRevision);
        result.setCreateDate(createDate);
        result.setLastOpDate(lastOpDate);
        result.setPolicyPlanHash(policyPlanHash);
        result.setExpectedControlOperationUuid(expectedControlOperationUuid);
        result.setReconcileOperationUuid(reconcileOperationUuid);
        return result;
    }
}
