package org.zstack.sdk.identity.role;

import org.zstack.sdk.identity.role.RoleType;
import org.zstack.sdk.identity.role.RoleState;

public class RoleInventory  {

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

    public java.lang.String identity;
    public void setIdentity(java.lang.String identity) {
        this.identity = identity;
    }
    public java.lang.String getIdentity() {
        return this.identity;
    }

    public RoleType type;
    public void setType(RoleType type) {
        this.type = type;
    }
    public RoleType getType() {
        return this.type;
    }

    public RoleState state;
    public void setState(RoleState state) {
        this.state = state;
    }
    public RoleState getState() {
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

    public java.util.List statements;
    public void setStatements(java.util.List statements) {
        this.statements = statements;
    }
    public java.util.List getStatements() {
        return this.statements;
    }

    public java.util.List policies;
    public void setPolicies(java.util.List policies) {
        this.policies = policies;
    }
    public java.util.List getPolicies() {
        return this.policies;
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
