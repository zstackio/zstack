package org.zstack.sdk;



public class GpuDeviceSpecCandidateInventory extends org.zstack.sdk.GpuDeviceSpecInventory {

    public java.lang.String mode;
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }
    public java.lang.String getMode() {
        return this.mode;
    }

    public java.lang.Long maxAvailableMemory;
    public void setMaxAvailableMemory(java.lang.Long maxAvailableMemory) {
        this.maxAvailableMemory = maxAvailableMemory;
    }
    public java.lang.Long getMaxAvailableMemory() {
        return this.maxAvailableMemory;
    }

    public java.util.List dgpuProfiles;
    public void setDgpuProfiles(java.util.List dgpuProfiles) {
        this.dgpuProfiles = dgpuProfiles;
    }
    public java.util.List getDgpuProfiles() {
        return this.dgpuProfiles;
    }

}
