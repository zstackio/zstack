package org.zstack.sdk;

import org.zstack.sdk.SdnControllerStatus;

public class SdnControllerInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String vendorType;
    public void setVendorType(java.lang.String vendorType) {
        this.vendorType = vendorType;
    }
    public java.lang.String getVendorType() {
        return this.vendorType;
    }

    public java.lang.String vendorVersion;
    public void setVendorVersion(java.lang.String vendorVersion) {
        this.vendorVersion = vendorVersion;
    }
    public java.lang.String getVendorVersion() {
        return this.vendorVersion;
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

    public java.lang.String ip;
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }
    public java.lang.String getIp() {
        return this.ip;
    }

    public java.lang.String username;
    public void setUsername(java.lang.String username) {
        this.username = username;
    }
    public java.lang.String getUsername() {
        return this.username;
    }

    public java.lang.String password;
    public void setPassword(java.lang.String password) {
        this.password = password;
    }
    public java.lang.String getPassword() {
        return this.password;
    }

    public SdnControllerStatus status;
    public void setStatus(SdnControllerStatus status) {
        this.status = status;
    }
    public SdnControllerStatus getStatus() {
        return this.status;
    }

    public java.util.List hostRefs;
    public void setHostRefs(java.util.List hostRefs) {
        this.hostRefs = hostRefs;
    }
    public java.util.List getHostRefs() {
        return this.hostRefs;
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
