package org.zstack.sdk;



public class FaultToleranceVmGroupInventory extends org.zstack.sdk.VmInstanceInventory {

    public java.lang.String primaryVmInstanceUuid;
    public void setPrimaryVmInstanceUuid(java.lang.String primaryVmInstanceUuid) {
        this.primaryVmInstanceUuid = primaryVmInstanceUuid;
    }
    public java.lang.String getPrimaryVmInstanceUuid() {
        return this.primaryVmInstanceUuid;
    }

    public java.lang.String secondaryVmInstanceUuid;
    public void setSecondaryVmInstanceUuid(java.lang.String secondaryVmInstanceUuid) {
        this.secondaryVmInstanceUuid = secondaryVmInstanceUuid;
    }
    public java.lang.String getSecondaryVmInstanceUuid() {
        return this.secondaryVmInstanceUuid;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

}
