package org.zstack.sdk;



public class VolumeExternalBackupInfo extends org.zstack.sdk.ResourceExternalBackupInfo {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public long size;
    public void setSize(long size) {
        this.size = size;
    }
    public long getSize() {
        return this.size;
    }

}
