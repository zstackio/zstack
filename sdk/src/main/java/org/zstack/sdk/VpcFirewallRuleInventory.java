package org.zstack.sdk;

import org.zstack.sdk.ActionType;
import org.zstack.sdk.ProtocolType;
import org.zstack.sdk.FirewallRuleState;

public class VpcFirewallRuleInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String vpcFirewallUuid;
    public void setVpcFirewallUuid(java.lang.String vpcFirewallUuid) {
        this.vpcFirewallUuid = vpcFirewallUuid;
    }
    public java.lang.String getVpcFirewallUuid() {
        return this.vpcFirewallUuid;
    }

    public java.lang.String ruleSetUuid;
    public void setRuleSetUuid(java.lang.String ruleSetUuid) {
        this.ruleSetUuid = ruleSetUuid;
    }
    public java.lang.String getRuleSetUuid() {
        return this.ruleSetUuid;
    }

    public ActionType action;
    public void setAction(ActionType action) {
        this.action = action;
    }
    public ActionType getAction() {
        return this.action;
    }

    public java.lang.String ruleSetName;
    public void setRuleSetName(java.lang.String ruleSetName) {
        this.ruleSetName = ruleSetName;
    }
    public java.lang.String getRuleSetName() {
        return this.ruleSetName;
    }

    public ProtocolType protocol;
    public void setProtocol(ProtocolType protocol) {
        this.protocol = protocol;
    }
    public ProtocolType getProtocol() {
        return this.protocol;
    }

    public java.lang.String destPort;
    public void setDestPort(java.lang.String destPort) {
        this.destPort = destPort;
    }
    public java.lang.String getDestPort() {
        return this.destPort;
    }

    public java.lang.String sourcePort;
    public void setSourcePort(java.lang.String sourcePort) {
        this.sourcePort = sourcePort;
    }
    public java.lang.String getSourcePort() {
        return this.sourcePort;
    }

    public java.lang.String sourceIp;
    public void setSourceIp(java.lang.String sourceIp) {
        this.sourceIp = sourceIp;
    }
    public java.lang.String getSourceIp() {
        return this.sourceIp;
    }

    public java.lang.String destIp;
    public void setDestIp(java.lang.String destIp) {
        this.destIp = destIp;
    }
    public java.lang.String getDestIp() {
        return this.destIp;
    }

    public java.lang.Integer ruleNumber;
    public void setRuleNumber(java.lang.Integer ruleNumber) {
        this.ruleNumber = ruleNumber;
    }
    public java.lang.Integer getRuleNumber() {
        return this.ruleNumber;
    }

    public java.lang.String allowStates;
    public void setAllowStates(java.lang.String allowStates) {
        this.allowStates = allowStates;
    }
    public java.lang.String getAllowStates() {
        return this.allowStates;
    }

    public java.lang.String tcpFlag;
    public void setTcpFlag(java.lang.String tcpFlag) {
        this.tcpFlag = tcpFlag;
    }
    public java.lang.String getTcpFlag() {
        return this.tcpFlag;
    }

    public java.lang.String icmpTypeName;
    public void setIcmpTypeName(java.lang.String icmpTypeName) {
        this.icmpTypeName = icmpTypeName;
    }
    public java.lang.String getIcmpTypeName() {
        return this.icmpTypeName;
    }

    public FirewallRuleState state;
    public void setState(FirewallRuleState state) {
        this.state = state;
    }
    public FirewallRuleState getState() {
        return this.state;
    }

    public boolean isDefault;
    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    public boolean getIsDefault() {
        return this.isDefault;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
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
