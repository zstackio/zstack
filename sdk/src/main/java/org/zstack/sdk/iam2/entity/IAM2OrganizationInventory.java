package org.zstack.sdk.iam2.entity;

import org.zstack.sdk.iam2.entity.IAM2State;
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

    public IAM2State state;
    public void setState(IAM2State state) {
        this.state = state;
    }
    public IAM2State getState() {
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

    public java.lang.String sourceCategory;
    public void setSourceCategory(java.lang.String sourceCategory) {
        this.sourceCategory = sourceCategory;
    }
    public java.lang.String getSourceCategory() {
        return this.sourceCategory;
    }

    public java.lang.String sourceType;
    public void setSourceType(java.lang.String sourceType) {
        this.sourceType = sourceType;
    }
    public java.lang.String getSourceType() {
        return this.sourceType;
    }

    public java.lang.String sourceName;
    public void setSourceName(java.lang.String sourceName) {
        this.sourceName = sourceName;
    }
    public java.lang.String getSourceName() {
        return this.sourceName;
    }

    public java.lang.String syncType;
    public void setSyncType(java.lang.String syncType) {
        this.syncType = syncType;
    }
    public java.lang.String getSyncType() {
        return this.syncType;
    }

    public java.lang.String externalUuid;
    public void setExternalUuid(java.lang.String externalUuid) {
        this.externalUuid = externalUuid;
    }
    public java.lang.String getExternalUuid() {
        return this.externalUuid;
    }

    public java.lang.String externalType;
    public void setExternalType(java.lang.String externalType) {
        this.externalType = externalType;
    }
    public java.lang.String getExternalType() {
        return this.externalType;
    }

}
