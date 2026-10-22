package org.zstack.sdk;



public class SecurityGroupRuleAO  {

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String remoteSecurityGroupUuid;
    public void setRemoteSecurityGroupUuid(java.lang.String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }
    public java.lang.String getRemoteSecurityGroupUuid() {
        return this.remoteSecurityGroupUuid;
    }

    public java.lang.Integer ipVersion;
    public void setIpVersion(java.lang.Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
    public java.lang.Integer getIpVersion() {
        return this.ipVersion;
    }

    public java.lang.String protocol;
    public void setProtocol(java.lang.String protocol) {
        this.protocol = protocol;
    }
    public java.lang.String getProtocol() {
        return this.protocol;
    }

    public java.lang.String srcIpRange;
    public void setSrcIpRange(java.lang.String srcIpRange) {
        this.srcIpRange = srcIpRange;
    }
    public java.lang.String getSrcIpRange() {
        return this.srcIpRange;
    }

    public java.lang.String dstIpRange;
    public void setDstIpRange(java.lang.String dstIpRange) {
        this.dstIpRange = dstIpRange;
    }
    public java.lang.String getDstIpRange() {
        return this.dstIpRange;
    }

    public java.lang.String dstPortRange;
    public void setDstPortRange(java.lang.String dstPortRange) {
        this.dstPortRange = dstPortRange;
    }
    public java.lang.String getDstPortRange() {
        return this.dstPortRange;
    }

    public java.lang.String action;
    public void setAction(java.lang.String action) {
        this.action = action;
    }
    public java.lang.String getAction() {
        return this.action;
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

    public java.lang.String allowedCidr;
    public void setAllowedCidr(java.lang.String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }
    public java.lang.String getAllowedCidr() {
        return this.allowedCidr;
    }

}
