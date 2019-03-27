package org.zstack.sdk.identity.role;



public class RolePolicyRefInventory  {

    public java.lang.String roleUuid;
    public void setRoleUuid(java.lang.String roleUuid) {
        this.roleUuid = roleUuid;
    }
    public java.lang.String getRoleUuid() {
        return this.roleUuid;
    }

    public java.lang.String policyUuid;
    public void setPolicyUuid(java.lang.String policyUuid) {
        this.policyUuid = policyUuid;
    }
    public java.lang.String getPolicyUuid() {
        return this.policyUuid;
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
