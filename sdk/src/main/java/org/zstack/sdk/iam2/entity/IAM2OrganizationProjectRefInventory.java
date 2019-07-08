package org.zstack.sdk.iam2.entity;



public class IAM2OrganizationProjectRefInventory  {

    public java.lang.String projectUuid;
    public void setProjectUuid(java.lang.String projectUuid) {
        this.projectUuid = projectUuid;
    }
    public java.lang.String getProjectUuid() {
        return this.projectUuid;
    }

    public java.lang.String organizationUuid;
    public void setOrganizationUuid(java.lang.String organizationUuid) {
        this.organizationUuid = organizationUuid;
    }
    public java.lang.String getOrganizationUuid() {
        return this.organizationUuid;
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
