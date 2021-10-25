package org.zstack.sdk.iam2.entity;

import org.zstack.sdk.iam2.entity.State;
import org.zstack.sdk.iam2.entity.OrganizationType;

public class IAM2OrganizationInventory  {

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

    public State state;
    public void setState(State state) {
        this.state = state;
    }
    public State getState() {
        return this.state;
    }

    public OrganizationType type;
    public void setType(OrganizationType type) {
        this.type = type;
    }
    public OrganizationType getType() {
        return this.type;
    }

    public java.lang.String srcType;
    public void setSrcType(java.lang.String srcType) {
        this.srcType = srcType;
    }
    public java.lang.String getSrcType() {
        return this.srcType;
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

    public java.lang.String parentUuid;
    public void setParentUuid(java.lang.String parentUuid) {
        this.parentUuid = parentUuid;
    }
    public java.lang.String getParentUuid() {
        return this.parentUuid;
    }

    public java.lang.String rootOrganizationUuid;
    public void setRootOrganizationUuid(java.lang.String rootOrganizationUuid) {
        this.rootOrganizationUuid = rootOrganizationUuid;
    }
    public java.lang.String getRootOrganizationUuid() {
        return this.rootOrganizationUuid;
    }

    public java.util.List attributes;
    public void setAttributes(java.util.List attributes) {
        this.attributes = attributes;
    }
    public java.util.List getAttributes() {
        return this.attributes;
    }

    public java.lang.String organizationRootPath;
    public void setOrganizationDetail(java.lang.String organizationDetail) {
        this.organizationRootPath = organizationDetail;
    }
    public java.lang.String getOrganizationDetail() {
        return this.organizationRootPath;
    }

    public java.lang.Long organizationId;
    public void setOrganizationId(java.lang.Long organizationId) {
        this.organizationId = organizationId;
    }
    public java.lang.Long getOrganizationId() {
        return this.organizationId;
    }

}
