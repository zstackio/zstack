package org.zstack.sdk;



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

    public java.util.List vniRanges;
    public void setVniRanges(java.util.List vniRanges) {
        this.vniRanges = vniRanges;
    }
    public java.util.List getVniRanges() {
        return this.vniRanges;
    }

    public java.util.List vxlanPools;
    public void setVxlanPools(java.util.List vxlanPools) {
        this.vxlanPools = vxlanPools;
    }
    public java.util.List getVxlanPools() {
        return this.vxlanPools;
    }

}
