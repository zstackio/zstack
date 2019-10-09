package org.zstack.sdk;



public class MulticastRouterInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
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

    public java.util.List rpGroups;
    public void setRpGroups(java.util.List rpGroups) {
        this.rpGroups = rpGroups;
    }
    public java.util.List getRpGroups() {
        return this.rpGroups;
    }

    public java.util.List vpcVrs;
    public void setVpcVrs(java.util.List vpcVrs) {
        this.vpcVrs = vpcVrs;
    }
    public java.util.List getVpcVrs() {
        return this.vpcVrs;
    }

}
