package org.zstack.sdk;



public class HostMemoryStateInventory  {

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String featureState;
    public void setFeatureState(java.lang.String featureState) {
        this.featureState = featureState;
    }
    public java.lang.String getFeatureState() {
        return this.featureState;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

    public java.util.Map capabilities;
    public void setCapabilities(java.util.Map capabilities) {
        this.capabilities = capabilities;
    }
    public java.util.Map getCapabilities() {
        return this.capabilities;
    }

    public java.util.List allowedActions;
    public void setAllowedActions(java.util.List allowedActions) {
        this.allowedActions = allowedActions;
    }
    public java.util.List getAllowedActions() {
        return this.allowedActions;
    }

    public java.lang.String activeJobUuid;
    public void setActiveJobUuid(java.lang.String activeJobUuid) {
        this.activeJobUuid = activeJobUuid;
    }
    public java.lang.String getActiveJobUuid() {
        return this.activeJobUuid;
    }

    public java.lang.Long lastObservedAt;
    public void setLastObservedAt(java.lang.Long lastObservedAt) {
        this.lastObservedAt = lastObservedAt;
    }
    public java.lang.Long getLastObservedAt() {
        return this.lastObservedAt;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
