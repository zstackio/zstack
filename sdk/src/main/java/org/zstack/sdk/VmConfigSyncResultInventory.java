package org.zstack.sdk;

import org.zstack.sdk.VmConfigType;

public class VmConfigSyncResultInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public VmConfigType type;
    public void setType(VmConfigType type) {
        this.type = type;
    }
    public VmConfigType getType() {
        return this.type;
    }

    public java.lang.Boolean success;
    public void setSuccess(java.lang.Boolean success) {
        this.success = success;
    }
    public java.lang.Boolean getSuccess() {
        return this.success;
    }

    public java.lang.String errCode;
    public void setErrCode(java.lang.String errCode) {
        this.errCode = errCode;
    }
    public java.lang.String getErrCode() {
        return this.errCode;
    }

}
