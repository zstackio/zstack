package org.zstack.sdk;



public class HostNetworkInterfaceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String bondingUuid;
    public void setBondingUuid(java.lang.String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }
    public java.lang.String getBondingUuid() {
        return this.bondingUuid;
    }

    public java.lang.String interfaceName;
    public void setInterfaceName(java.lang.String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public java.lang.String getInterfaceName() {
        return this.interfaceName;
    }

    public java.lang.String interfaceType;
    public void setInterfaceType(java.lang.String interfaceType) {
        this.interfaceType = interfaceType;
    }
    public java.lang.String getInterfaceType() {
        return this.interfaceType;
    }

    public java.lang.Long speed;
    public void setSpeed(java.lang.Long speed) {
        this.speed = speed;
    }
    public java.lang.Long getSpeed() {
        return this.speed;
    }

    public boolean slaveActive;
    public void setSlaveActive(boolean slaveActive) {
        this.slaveActive = slaveActive;
    }
    public boolean getSlaveActive() {
        return this.slaveActive;
    }

    public boolean carrierActive;
    public void setCarrierActive(boolean carrierActive) {
        this.carrierActive = carrierActive;
    }
    public boolean getCarrierActive() {
        return this.carrierActive;
    }

    public java.util.List ipAddresses;
    public void setIpAddresses(java.util.List ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
    public java.util.List getIpAddresses() {
        return this.ipAddresses;
    }

    public java.lang.String mac;
    public void setMac(java.lang.String mac) {
        this.mac = mac;
    }
    public java.lang.String getMac() {
        return this.mac;
    }

    public java.lang.String pciDeviceAddress;
    public void setPciDeviceAddress(java.lang.String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }
    public java.lang.String getPciDeviceAddress() {
        return this.pciDeviceAddress;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
