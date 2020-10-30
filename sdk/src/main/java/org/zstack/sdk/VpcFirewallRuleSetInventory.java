package org.zstack.sdk;

import org.zstack.sdk.ActionType;

public class VpcFirewallRuleSetInventory  {

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

    public ActionType actionType;
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    public ActionType getActionType() {
        return this.actionType;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public boolean isDefault;
    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    public boolean getIsDefault() {
        return this.isDefault;
    }

    public boolean isApplied;
    public void setIsApplied(boolean isApplied) {
        this.isApplied = isApplied;
    }
    public boolean getIsApplied() {
        return this.isApplied;
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

    public java.util.List rules;
    public void setRules(java.util.List rules) {
        this.rules = rules;
    }
    public java.util.List getRules() {
        return this.rules;
    }

}
