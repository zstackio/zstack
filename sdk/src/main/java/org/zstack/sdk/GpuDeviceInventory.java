package org.zstack.sdk;

import org.zstack.sdk.GpuAllocateStatus;
import org.zstack.sdk.DeviceScope;

public class GpuDeviceInventory extends org.zstack.sdk.PciDeviceInventory {

    public java.lang.String serialNumber;
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public java.lang.String getSerialNumber() {
        return this.serialNumber;
    }

    public java.lang.Long memory;
    public void setMemory(java.lang.Long memory) {
        this.memory = memory;
    }
    public java.lang.Long getMemory() {
        return this.memory;
    }

    public java.lang.Long power;
    public void setPower(java.lang.Long power) {
        this.power = power;
    }
    public java.lang.Long getPower() {
        return this.power;
    }

    public java.lang.Boolean isDriverLoaded;
    public void setIsDriverLoaded(java.lang.Boolean isDriverLoaded) {
        this.isDriverLoaded = isDriverLoaded;
    }
    public java.lang.Boolean getIsDriverLoaded() {
        return this.isDriverLoaded;
    }

    public java.lang.String gpuType;
    public void setGpuType(java.lang.String gpuType) {
        this.gpuType = gpuType;
    }
    public java.lang.String getGpuType() {
        return this.gpuType;
    }

    public java.lang.String gpuStatus;
    public void setGpuStatus(java.lang.String gpuStatus) {
        this.gpuStatus = gpuStatus;
    }
    public java.lang.String getGpuStatus() {
        return this.gpuStatus;
    }

    public GpuAllocateStatus allocateStatus;
    public void setAllocateStatus(GpuAllocateStatus allocateStatus) {
        this.allocateStatus = allocateStatus;
    }
    public GpuAllocateStatus getAllocateStatus() {
        return this.allocateStatus;
    }

    public DeviceScope scope;
    public void setScope(DeviceScope scope) {
        this.scope = scope;
    }
    public DeviceScope getScope() {
        return this.scope;
    }

    public java.lang.String chassisUuid;
    public void setChassisUuid(java.lang.String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }
    public java.lang.String getChassisUuid() {
        return this.chassisUuid;
    }

}
