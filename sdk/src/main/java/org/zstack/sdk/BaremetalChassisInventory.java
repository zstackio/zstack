package org.zstack.sdk;

public class BaremetalChassisInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String ipmiAddress;
    public void setIpmiAddress(java.lang.String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }
    public java.lang.String getIpmiAddress() {
        return this.ipmiAddress;
    }

    public java.lang.String ipmiUsername;
    public void setIpmiUsername(java.lang.String ipmiUsername) {
        this.ipmiUsername = ipmiUsername;
    }
    public java.lang.String getIpmiUsername() {
        return this.ipmiUsername;
    }

    public java.lang.String ipmiPassword;
    public void setIpmiPassword(java.lang.String ipmiPassword) {
        this.ipmiPassword = ipmiPassword;
    }
    public java.lang.String getIpmiPassword() {
        return this.ipmiPassword;
    }

    public java.lang.Boolean provisioned;
    public void setProvisioned(java.lang.Boolean provisioned) {
        this.provisioned = provisioned;
    }
    public java.lang.Boolean getProvisioned() {
        return this.provisioned;
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
