package org.zstack.sdk.iam2.entity;

import org.zstack.sdk.iam2.entity.ProjectState;

public class IAM2ProjectInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
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

    public ProjectState state;
    public void setState(ProjectState state) {
        this.state = state;
    }
    public ProjectState getState() {
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

    public java.util.List attributes;
    public void setAttributes(java.util.List attributes) {
        this.attributes = attributes;
    }
    public java.util.List getAttributes() {
        return this.attributes;
    }

    public java.lang.String linkedAccountUuid;
    public void setLinkedAccountUuid(java.lang.String linkedAccountUuid) {
        this.linkedAccountUuid = linkedAccountUuid;
    }
    public java.lang.String getLinkedAccountUuid() {
        return this.linkedAccountUuid;
    }

}
