package org.zstack.sdk;

import org.zstack.sdk.VmModelMountStatus;

public class VmModelMountInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String modelUuid;
    public void setModelUuid(java.lang.String modelUuid) {
        this.modelUuid = modelUuid;
    }
    public java.lang.String getModelUuid() {
        return this.modelUuid;
    }

    public java.lang.String modelName;
    public void setModelName(java.lang.String modelName) {
        this.modelName = modelName;
    }
    public java.lang.String getModelName() {
        return this.modelName;
    }

    public java.lang.String mountPath;
    public void setMountPath(java.lang.String mountPath) {
        this.mountPath = mountPath;
    }
    public java.lang.String getMountPath() {
        return this.mountPath;
    }

    public java.lang.String sourcePath;
    public void setSourcePath(java.lang.String sourcePath) {
        this.sourcePath = sourcePath;
    }
    public java.lang.String getSourcePath() {
        return this.sourcePath;
    }

    public java.lang.String cacheUuid;
    public void setCacheUuid(java.lang.String cacheUuid) {
        this.cacheUuid = cacheUuid;
    }
    public java.lang.String getCacheUuid() {
        return this.cacheUuid;
    }

    public VmModelMountStatus status;
    public void setStatus(VmModelMountStatus status) {
        this.status = status;
    }
    public VmModelMountStatus getStatus() {
        return this.status;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
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
