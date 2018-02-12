package org.zstack.sdk;

public class SecurityGroupRuleInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String securityGroupUuid;
    public void setSecurityGroupUuid(java.lang.String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
    public java.lang.String getSecurityGroupUuid() {
        return this.securityGroupUuid;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.Integer startPort;
    public void setStartPort(java.lang.Integer startPort) {
        this.startPort = startPort;
    }
    public java.lang.Integer getStartPort() {
        return this.startPort;
    }

    public java.lang.Integer endPort;
    public void setEndPort(java.lang.Integer endPort) {
        this.endPort = endPort;
    }
    public java.lang.Integer getEndPort() {
        return this.endPort;
    }

    public java.lang.String protocol;
    public void setProtocol(java.lang.String protocol) {
        this.protocol = protocol;
    }
    public java.lang.String getProtocol() {
        return this.protocol;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String allowedCidr;
    public void setAllowedCidr(java.lang.String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }
    public java.lang.String getAllowedCidr() {
        return this.allowedCidr;
    }

    public java.lang.String remoteSecurityGroupUuid;
    public void setRemoteSecurityGroupUuid(java.lang.String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }
    public java.lang.String getRemoteSecurityGroupUuid() {
        return this.remoteSecurityGroupUuid;
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
