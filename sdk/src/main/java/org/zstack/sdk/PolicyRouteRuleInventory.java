package org.zstack.sdk;

import org.zstack.sdk.PolicyRouteRuleProtocol;
import org.zstack.sdk.PolicyRouteRuleState;

public class PolicyRouteRuleInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public int ruleNumber;
    public void setRuleNumber(int ruleNumber) {
        this.ruleNumber = ruleNumber;
    }
    public int getRuleNumber() {
        return this.ruleNumber;
    }

    public java.lang.String ruleSetUuid;
    public void setRuleSetUuid(java.lang.String ruleSetUuid) {
        this.ruleSetUuid = ruleSetUuid;
    }
    public java.lang.String getRuleSetUuid() {
        return this.ruleSetUuid;
    }

    public java.lang.String tableUuid;
    public void setTableUuid(java.lang.String tableUuid) {
        this.tableUuid = tableUuid;
    }
    public java.lang.String getTableUuid() {
        return this.tableUuid;
    }

    public java.lang.String destIp;
    public void setDestIp(java.lang.String destIp) {
        this.destIp = destIp;
    }
    public java.lang.String getDestIp() {
        return this.destIp;
    }

    public java.lang.String sourceIp;
    public void setSourceIp(java.lang.String sourceIp) {
        this.sourceIp = sourceIp;
    }
    public java.lang.String getSourceIp() {
        return this.sourceIp;
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

    public PolicyRouteRuleProtocol protocol;
    public void setProtocol(PolicyRouteRuleProtocol protocol) {
        this.protocol = protocol;
    }
    public PolicyRouteRuleProtocol getProtocol() {
        return this.protocol;
    }

    public PolicyRouteRuleState state;
    public void setState(PolicyRouteRuleState state) {
        this.state = state;
    }
    public PolicyRouteRuleState getState() {
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

}
