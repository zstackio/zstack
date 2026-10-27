package org.zstack.kvm.memory;

public class MemoryTaskInventory {
    private String expectedControlOperationUuid;
    public String getExpectedControlOperationUuid() { return expectedControlOperationUuid; }
    public void setExpectedControlOperationUuid(String value) { expectedControlOperationUuid = value; }

    private String reconcileOperationUuid;
    public String getReconcileOperationUuid() { return reconcileOperationUuid; }
    public void setReconcileOperationUuid(String value) { reconcileOperationUuid = value; }

    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    private String parentUuid;
    public String getParentUuid() { return parentUuid; }
    public void setParentUuid(String value) { parentUuid = value; }

    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    private String action;
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    private long desiredRevision;
    public long getDesiredRevision() { return desiredRevision; }
    public void setDesiredRevision(long value) { desiredRevision = value; }

    private String policyPlanHash;
    public String getPolicyPlanHash() { return policyPlanHash; }
    public void setPolicyPlanHash(String value) { policyPlanHash = value; }

    private java.sql.Timestamp createDate;
    public java.sql.Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(java.sql.Timestamp value) { createDate = value; }

    private java.sql.Timestamp lastOpDate;
    public java.sql.Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(java.sql.Timestamp value) { lastOpDate = value; }

}
