package org.zstack.sdk;



public class ZBoxVmBackupInfo extends org.zstack.sdk.ResourceExternalBackupInfo {

    public java.util.List volumes;
    public void setVolumes(java.util.List volumes) {
        this.volumes = volumes;
    }
    public java.util.List getVolumes() {
        return this.volumes;
    }

    public long totalSize;
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    public long getTotalSize() {
        return this.totalSize;
    }

}
