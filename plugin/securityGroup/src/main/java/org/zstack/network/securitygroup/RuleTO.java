package org.zstack.network.securitygroup;

import java.util.Collection;
import java.util.List;

public class RuleTO {
    private String protocol;
    private String type;
    private int startPort;
    private int endPort;
    private String allowedCidr;
    private String securityGroupUuid;
    private String remoteGroupUuid;
    private List<String> remoteGroupVmIps;
    
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public int getStartPort() {
        return startPort;
    }
    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }
    public int getEndPort() {
        return endPort;
    }
    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }
    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setRemoteGroupVmIps(List<String> remoteGroupVmIps) {
        this.remoteGroupVmIps = remoteGroupVmIps;
    }

    public String getRemoteGroupUuid() {
        return remoteGroupUuid;
    }

    public void setRemoteGroupUuid(String remoteGroupUuid) {
        this.remoteGroupUuid = remoteGroupUuid;
    }

    public List<String> getRemoteGroupVmIps() {
        return remoteGroupVmIps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("startPort: %s,", this.startPort));
        sb.append(String.format("endPort: %s,", this.endPort));
        sb.append(String.format("allowedCidr: %s", this.allowedCidr));
        sb.append(String.format("securityGroupUuid: %s", this.securityGroupUuid));
        sb.append(String.format("remoteGroupUuid: %s", this.remoteGroupUuid));
        sb.append(String.format("remoteGroupVmIps: %s", this.remoteGroupVmIps));
        return sb.toString();
    }
    
    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("startPort: %s,", this.startPort));
        sb.append(String.format("endPort: %s,", this.endPort));
        sb.append(String.format("allowedCidr: %s,", this.allowedCidr));
        sb.append(String.format("securityGroupUuid: %s", this.securityGroupUuid));
        sb.append(String.format("remoteGroupUuid: %s", this.remoteGroupUuid));
        sb.append(String.format("remoteGroupVmIps: %s", this.remoteGroupVmIps));
        return sb.toString();
    }
}
