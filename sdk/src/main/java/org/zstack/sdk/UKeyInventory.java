package org.zstack.sdk;

import org.zstack.sdk.UKeyStatus;

public class UKeyInventory  {

    public java.lang.String managementNodeUuid;
    public void setManagementNodeUuid(java.lang.String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
    public java.lang.String getManagementNodeUuid() {
        return this.managementNodeUuid;
    }

    public UKeyStatus status;
    public void setStatus(UKeyStatus status) {
        this.status = status;
    }
    public UKeyStatus getStatus() {
        return this.status;
    }

    public java.lang.String keyId;
    public void setKeyId(java.lang.String keyId) {
        this.keyId = keyId;
    }
    public java.lang.String getKeyId() {
        return this.keyId;
    }

}
