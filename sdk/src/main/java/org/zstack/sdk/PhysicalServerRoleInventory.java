package org.zstack.sdk;



public class PhysicalServerRoleInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String serverUuid;
    public void setServerUuid(java.lang.String serverUuid) {
        this.serverUuid = serverUuid;
    }
    public java.lang.String getServerUuid() {
        return this.serverUuid;
    }

    public java.lang.String roleType;
    public void setRoleType(java.lang.String roleType) {
        this.roleType = roleType;
    }
    public java.lang.String getRoleType() {
        return this.roleType;
    }

    public java.lang.String roleUuid;
    public void setRoleUuid(java.lang.String roleUuid) {
        this.roleUuid = roleUuid;
    }
    public java.lang.String getRoleUuid() {
        return this.roleUuid;
    }

    public java.lang.String schedulingMode;
    public void setSchedulingMode(java.lang.String schedulingMode) {
        this.schedulingMode = schedulingMode;
    }
    public java.lang.String getSchedulingMode() {
        return this.schedulingMode;
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
