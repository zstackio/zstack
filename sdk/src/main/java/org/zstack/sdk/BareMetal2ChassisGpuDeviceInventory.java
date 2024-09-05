package org.zstack.sdk;



public class BareMetal2ChassisGpuDeviceInventory extends org.zstack.sdk.BareMetal2ChassisPciDeviceInventory {

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

    public boolean isDriverLoaded;
    public void setIsDriverLoaded(boolean isDriverLoaded) {
        this.isDriverLoaded = isDriverLoaded;
    }
    public boolean getIsDriverLoaded() {
        return this.isDriverLoaded;
    }

}
