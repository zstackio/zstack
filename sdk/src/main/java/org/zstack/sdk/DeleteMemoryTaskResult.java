package org.zstack.sdk;



public class DeleteMemoryTaskResult {
    public java.lang.String taskUuid;
    public void setTaskUuid(java.lang.String taskUuid) {
        this.taskUuid = taskUuid;
    }
    public java.lang.String getTaskUuid() {
        return this.taskUuid;
    }

    public boolean deleted;
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    public boolean getDeleted() {
        return this.deleted;
    }

    public java.lang.String scope;
    public void setScope(java.lang.String scope) {
        this.scope = scope;
    }
    public java.lang.String getScope() {
        return this.scope;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.util.List hostUuids;
    public void setHostUuids(java.util.List hostUuids) {
        this.hostUuids = hostUuids;
    }
    public java.util.List getHostUuids() {
        return this.hostUuids;
    }

}
