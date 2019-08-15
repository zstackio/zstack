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

    public java.lang.String vpcFirewallUuid;
    public void setVpcFirewallUuid(java.lang.String vpcFirewallUuid) {
        this.vpcFirewallUuid = vpcFirewallUuid;
    }
    public java.lang.String getVpcFirewallUuid() {
        return this.vpcFirewallUuid;
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
