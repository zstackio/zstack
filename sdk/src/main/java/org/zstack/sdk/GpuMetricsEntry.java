package org.zstack.sdk;



public class GpuMetricsEntry  {

    public java.lang.String gpuDeviceUuid;
    public void setGpuDeviceUuid(java.lang.String gpuDeviceUuid) {
        this.gpuDeviceUuid = gpuDeviceUuid;
    }
    public java.lang.String getGpuDeviceUuid() {
        return this.gpuDeviceUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String pciDeviceAddress;
    public void setPciDeviceAddress(java.lang.String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }
    public java.lang.String getPciDeviceAddress() {
        return this.pciDeviceAddress;
    }

    public java.lang.Double utilization;
    public void setUtilization(java.lang.Double utilization) {
        this.utilization = utilization;
    }
    public java.lang.Double getUtilization() {
        return this.utilization;
    }

    public java.lang.Double memoryUtilization;
    public void setMemoryUtilization(java.lang.Double memoryUtilization) {
        this.memoryUtilization = memoryUtilization;
    }
    public java.lang.Double getMemoryUtilization() {
        return this.memoryUtilization;
    }

    public java.lang.Double temperature;
    public void setTemperature(java.lang.Double temperature) {
        this.temperature = temperature;
    }
    public java.lang.Double getTemperature() {
        return this.temperature;
    }

    public java.lang.Double powerDraw;
    public void setPowerDraw(java.lang.Double powerDraw) {
        this.powerDraw = powerDraw;
    }
    public java.lang.Double getPowerDraw() {
        return this.powerDraw;
    }

    public java.lang.Double fanSpeed;
    public void setFanSpeed(java.lang.Double fanSpeed) {
        this.fanSpeed = fanSpeed;
    }
    public java.lang.Double getFanSpeed() {
        return this.fanSpeed;
    }

    public java.lang.String gpuStatus;
    public void setGpuStatus(java.lang.String gpuStatus) {
        this.gpuStatus = gpuStatus;
    }
    public java.lang.String getGpuStatus() {
        return this.gpuStatus;
    }

    public java.util.Map extraMetrics;
    public void setExtraMetrics(java.util.Map extraMetrics) {
        this.extraMetrics = extraMetrics;
    }
    public java.util.Map getExtraMetrics() {
        return this.extraMetrics;
    }

}
