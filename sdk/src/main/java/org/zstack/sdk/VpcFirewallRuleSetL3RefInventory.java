package org.zstack.sdk;

import org.zstack.sdk.PacketsForwardType;

public class VpcFirewallRuleSetL3RefInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String ruleSetUuid;
    public void setRuleSetUuid(java.lang.String ruleSetUuid) {
        this.ruleSetUuid = ruleSetUuid;
    }
    public java.lang.String getRuleSetUuid() {
        return this.ruleSetUuid;
    }

    public java.lang.String l3NetworkUuid;
    public void setL3NetworkUuid(java.lang.String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public java.lang.String getL3NetworkUuid() {
        return this.l3NetworkUuid;
    }

    public java.lang.String vpcFirewallUuid;
    public void setVpcFirewallUuid(java.lang.String vpcFirewallUuid) {
        this.vpcFirewallUuid = vpcFirewallUuid;
    }
    public java.lang.String getVpcFirewallUuid() {
        return this.vpcFirewallUuid;
    }

    public PacketsForwardType packetsForwardType;
    public void setPacketsForwardType(PacketsForwardType packetsForwardType) {
        this.packetsForwardType = packetsForwardType;
    }
    public PacketsForwardType getPacketsForwardType() {
        return this.packetsForwardType;
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
