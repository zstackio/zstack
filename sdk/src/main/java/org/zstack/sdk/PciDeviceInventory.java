package org.zstack.sdk;

public class PciDeviceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
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

    public PciDeviceStatus status;
    public void setStatus(PciDeviceStatus status) {
        this.status = status;
    }
    public PciDeviceStatus getStatus() {
        return this.status;
    }

    public PciDeviceState state;
    public void setState(PciDeviceState state) {
        this.state = state;
    }
    public PciDeviceState getState() {
        return this.state;
    }

    public PciDeviceType type;
    public void setType(PciDeviceType type) {
        this.type = type;
    }
    public PciDeviceType getType() {
        return this.type;
    }

    public java.lang.String vendorId;
    public void setVendorId(java.lang.String vendorId) {
        this.vendorId = vendorId;
    }
    public java.lang.String getVendorId() {
        return this.vendorId;
    }

    public java.lang.String deviceId;
    public void setDeviceId(java.lang.String deviceId) {
        this.deviceId = deviceId;
    }
    public java.lang.String getDeviceId() {
        return this.deviceId;
    }

    public java.lang.String subvendorId;
    public void setSubvendorId(java.lang.String subvendorId) {
        this.subvendorId = subvendorId;
    }
    public java.lang.String getSubvendorId() {
        return this.subvendorId;
    }

    public java.lang.String subdeviceId;
    public void setSubdeviceId(java.lang.String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }
    public java.lang.String getSubdeviceId() {
        return this.subdeviceId;
    }

    public PciDeviceMetaData metaData;
    public void setMetaData(PciDeviceMetaData metaData) {
        this.metaData = metaData;
    }
    public PciDeviceMetaData getMetaData() {
        return this.metaData;
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

    public java.util.List<PciDevicePciDeviceOfferingRefInventory> matchedPciDeviceOfferingRef;
    public void setMatchedPciDeviceOfferingRef(java.util.List<PciDevicePciDeviceOfferingRefInventory> matchedPciDeviceOfferingRef) {
        this.matchedPciDeviceOfferingRef = matchedPciDeviceOfferingRef;
    }
    public java.util.List<PciDevicePciDeviceOfferingRefInventory> getMatchedPciDeviceOfferingRef() {
        return this.matchedPciDeviceOfferingRef;
    }

}
