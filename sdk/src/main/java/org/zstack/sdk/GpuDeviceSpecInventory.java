package org.zstack.sdk;



public class GpuDeviceSpecInventory extends org.zstack.sdk.PciDeviceSpecInventory {

    public java.lang.Long memory;
    public void setMemory(java.lang.Long memory) {
        this.memory = memory;
    }
    public java.lang.Long getMemory() {
        return this.memory;
    }

    public java.lang.String gpuType;
    public void setGpuType(java.lang.String gpuType) {
        this.gpuType = gpuType;
    }
    public java.lang.String getGpuType() {
        return this.gpuType;
    }

    public java.lang.Boolean isolated;
    public void setIsolated(java.lang.Boolean isolated) {
        this.isolated = isolated;
    }
    public java.lang.Boolean getIsolated() {
        return this.isolated;
    }

}
