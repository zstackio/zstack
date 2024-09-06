package org.zstack.sdk;

import org.zstack.sdk.VmInstanceInventory;

public class ModelServiceInstanceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String modelServiceGroupUuid;
    public void setModelServiceGroupUuid(java.lang.String modelServiceGroupUuid) {
        this.modelServiceGroupUuid = modelServiceGroupUuid;
    }
    public java.lang.String getModelServiceGroupUuid() {
        return this.modelServiceGroupUuid;
    }

    public java.lang.String yaml;
    public void setYaml(java.lang.String yaml) {
        this.yaml = yaml;
    }
    public java.lang.String getYaml() {
        return this.yaml;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String url;
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
    public java.lang.String getUrl() {
        return this.url;
    }

    public java.lang.String internalUrl;
    public void setInternalUrl(java.lang.String internalUrl) {
        this.internalUrl = internalUrl;
    }
    public java.lang.String getInternalUrl() {
        return this.internalUrl;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public VmInstanceInventory vm;
    public void setVm(VmInstanceInventory vm) {
        this.vm = vm;
    }
    public VmInstanceInventory getVm() {
        return this.vm;
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
