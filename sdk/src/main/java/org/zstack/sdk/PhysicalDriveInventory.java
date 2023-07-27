package org.zstack.sdk;

import org.zstack.sdk.StorageDeviceTransferProtocol;

public class PhysicalDriveInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String wwn;
    public void setWwn(java.lang.String wwn) {
        this.wwn = wwn;
    }
    public java.lang.String getWwn() {
        return this.wwn;
    }

    public java.lang.String serialNumber;
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public java.lang.String getSerialNumber() {
        return this.serialNumber;
    }

    public java.lang.String deviceModel;
    public void setDeviceModel(java.lang.String deviceModel) {
        this.deviceModel = deviceModel;
    }
    public java.lang.String getDeviceModel() {
        return this.deviceModel;
    }

    public java.lang.Long size;
    public void setSize(java.lang.Long size) {
        this.size = size;
    }
    public java.lang.Long getSize() {
        return this.size;
    }

    public java.lang.String driveType;
    public void setDriveType(java.lang.String driveType) {
        this.driveType = driveType;
    }
    public java.lang.String getDriveType() {
        return this.driveType;
    }

    public StorageDeviceTransferProtocol protocol;
    public void setProtocol(StorageDeviceTransferProtocol protocol) {
        this.protocol = protocol;
    }
    public StorageDeviceTransferProtocol getProtocol() {
        return this.protocol;
    }

    public java.lang.String mediaType;
    public void setMediaType(java.lang.String mediaType) {
        this.mediaType = mediaType;
    }
    public java.lang.String getMediaType() {
        return this.mediaType;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
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
