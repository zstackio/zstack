package org.zstack.sdk;



public class SlbGroupMonitorIpInventory  {

    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String slbGroupUuid;
    public void setSlbGroupUuid(java.lang.String slbGroupUuid) {
        this.slbGroupUuid = slbGroupUuid;
    }
    public java.lang.String getSlbGroupUuid() {
        return this.slbGroupUuid;
    }

    public java.lang.String monitorIp;
    public void setMonitorIp(java.lang.String monitorIp) {
        this.monitorIp = monitorIp;
    }
    public java.lang.String getMonitorIp() {
        return this.monitorIp;
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
