package org.zstack.sdk;

import org.zstack.sdk.HygonDeviceType;
import org.zstack.sdk.HygonDeviceState;

public class HygonDeviceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
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

    public java.lang.String pciBdf;
    public void setPciBdf(java.lang.String pciBdf) {
        this.pciBdf = pciBdf;
    }
    public java.lang.String getPciBdf() {
        return this.pciBdf;
    }

    public HygonDeviceType deviceType;
    public void setDeviceType(HygonDeviceType deviceType) {
        this.deviceType = deviceType;
    }
    public HygonDeviceType getDeviceType() {
        return this.deviceType;
    }

    public java.lang.String deviceId;
    public void setDeviceId(java.lang.String deviceId) {
        this.deviceId = deviceId;
    }
    public java.lang.String getDeviceId() {
        return this.deviceId;
    }

    public java.lang.String driverStatus;
    public void setDriverStatus(java.lang.String driverStatus) {
        this.driverStatus = driverStatus;
    }
    public java.lang.String getDriverStatus() {
        return this.driverStatus;
    }

    public java.lang.Boolean isMasterPsp;
    public void setIsMasterPsp(java.lang.Boolean isMasterPsp) {
        this.isMasterPsp = isMasterPsp;
    }
    public java.lang.Boolean getIsMasterPsp() {
        return this.isMasterPsp;
    }

    public java.lang.Integer vendorIdx;
    public void setVendorIdx(java.lang.Integer vendorIdx) {
        this.vendorIdx = vendorIdx;
    }
    public java.lang.Integer getVendorIdx() {
        return this.vendorIdx;
    }

    public HygonDeviceState state;
    public void setState(HygonDeviceState state) {
        this.state = state;
    }
    public HygonDeviceState getState() {
        return this.state;
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
