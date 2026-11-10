package org.zstack.sdk;

import org.zstack.sdk.CryptoDeviceQualification;
import org.zstack.sdk.CryptoDeviceHealth;
import org.zstack.sdk.CryptoDeviceAllocateStatus;
import org.zstack.sdk.CryptoDeviceUnavailableReason;

public class CryptoDeviceInventory extends org.zstack.sdk.PciDeviceInventory {

    public java.lang.String driverVersion;
    public void setDriverVersion(java.lang.String driverVersion) {
        this.driverVersion = driverVersion;
    }
    public java.lang.String getDriverVersion() {
        return this.driverVersion;
    }

    public CryptoDeviceQualification qualification;
    public void setQualification(CryptoDeviceQualification qualification) {
        this.qualification = qualification;
    }
    public CryptoDeviceQualification getQualification() {
        return this.qualification;
    }

    public CryptoDeviceHealth healthStatus;
    public void setHealthStatus(CryptoDeviceHealth healthStatus) {
        this.healthStatus = healthStatus;
    }
    public CryptoDeviceHealth getHealthStatus() {
        return this.healthStatus;
    }

    public java.lang.String healthReason;
    public void setHealthReason(java.lang.String healthReason) {
        this.healthReason = healthReason;
    }
    public java.lang.String getHealthReason() {
        return this.healthReason;
    }

    public java.lang.String serialNumber;
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public java.lang.String getSerialNumber() {
        return this.serialNumber;
    }

    public java.lang.String firmwareVersion;
    public void setFirmwareVersion(java.lang.String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    public java.lang.String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public java.util.List supportedVfCounts;
    public void setSupportedVfCounts(java.util.List supportedVfCounts) {
        this.supportedVfCounts = supportedVfCounts;
    }
    public java.util.List getSupportedVfCounts() {
        return this.supportedVfCounts;
    }

    public CryptoDeviceAllocateStatus allocateStatus;
    public void setAllocateStatus(CryptoDeviceAllocateStatus allocateStatus) {
        this.allocateStatus = allocateStatus;
    }
    public CryptoDeviceAllocateStatus getAllocateStatus() {
        return this.allocateStatus;
    }

    public CryptoDeviceUnavailableReason unavailableReason;
    public void setUnavailableReason(CryptoDeviceUnavailableReason unavailableReason) {
        this.unavailableReason = unavailableReason;
    }
    public CryptoDeviceUnavailableReason getUnavailableReason() {
        return this.unavailableReason;
    }

}
