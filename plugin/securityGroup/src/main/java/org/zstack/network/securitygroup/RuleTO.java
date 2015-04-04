package org.zstack.network.securitygroup;

import java.util.Collection;

public class RuleTO {
    private String protocol;
    private String type;
    private int startPort;
    private int endPort;
    private Collection<String> allowedInternalIpRange;
    private String allowedCidr;
    
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
    public Collection<String> getAllowedInternalIpRange() {
        return allowedInternalIpRange;
    }
    public void setAllowedInternalIpRange(Collection<String> allowedInternalIpRange) {
        this.allowedInternalIpRange = allowedInternalIpRange;
    }
    public String getAllowedCidr() {
        return allowedCidr;
    }
    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("startPort: %s,", this.startPort));
        sb.append(String.format("endPort: %s,", this.endPort));
        sb.append(String.format("allowedCidr: %s", this.allowedCidr));
        return sb.toString();
    }
    
    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("startPort: %s,", this.startPort));
        sb.append(String.format("endPort: %s,", this.endPort));
        sb.append(String.format("allowedCidr: %s,", this.allowedCidr));
        sb.append(String.format("allowedInternalIpRange: %s", this.allowedInternalIpRange));
        return sb.toString();
    }
}
