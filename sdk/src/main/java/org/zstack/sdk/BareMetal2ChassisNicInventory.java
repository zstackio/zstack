package org.zstack.sdk;



public class BareMetal2ChassisNicInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String chassisUuid;
    public void setChassisUuid(java.lang.String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }
    public java.lang.String getChassisUuid() {
        return this.chassisUuid;
    }

    public java.lang.String mac;
    public void setMac(java.lang.String mac) {
        this.mac = mac;
    }
    public java.lang.String getMac() {
        return this.mac;
    }

    public java.lang.String nicName;
    public String getNicName() {
        return nicName;
    }
    public void setNicName(String nicName) {
        this.nicName = nicName;
    }

    public java.lang.String speed;
    public void setSpeed(java.lang.String speed) {
        this.speed = speed;
    }
    public java.lang.String getSpeed() {
        return this.speed;
    }

    public java.lang.Boolean isProvisionNic;
    public void setIsProvisionNic(java.lang.Boolean isProvisionNic) {
        this.isProvisionNic = isProvisionNic;
    }
    public java.lang.Boolean getIsProvisionNic() {
        return this.isProvisionNic;
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
