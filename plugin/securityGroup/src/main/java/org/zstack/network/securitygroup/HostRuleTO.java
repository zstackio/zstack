package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.List;

public class HostRuleTO {
    private List<SecurityGroupRuleTO> rules;
    private List<SecurityGroupRuleTO> ipv6Rules;
    private String hostUuid;
    private String hypervisorType;
    private boolean refreshHost;

    public HostRuleTO() {
        rules = new ArrayList<SecurityGroupRuleTO>();
        ipv6Rules = new ArrayList<SecurityGroupRuleTO>();
    }

    public List<SecurityGroupRuleTO> getRules() {
        if (rules == null) {
            rules = new ArrayList<SecurityGroupRuleTO>();
        }
        return rules;
    }
    public void setRules(List<SecurityGroupRuleTO> rules) {
        this.rules = rules;
    }
    public String getHostUuid() {
        return hostUuid;
    }
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public String getHypervisorType() {
        return hypervisorType;
    }
    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
    public boolean isRefreshHost() {
        return refreshHost;
    }
    public void setRefreshHost(boolean refreshHost) {
        this.refreshHost = refreshHost;
    }

    public void setActionCodeForAllSecurityGroupRuleTOs(String actionCode) {
        for (SecurityGroupRuleTO rto : rules) {
            rto.setActionCode(actionCode);
        }
        for (SecurityGroupRuleTO rto : ipv6Rules) {
            rto.setActionCode(actionCode);
        }
    }

    public List<SecurityGroupRuleTO> getIpv6Rules() {
        if (ipv6Rules == null) {
            ipv6Rules = new ArrayList<SecurityGroupRuleTO>();
        }
        return ipv6Rules;
    }

    public void setIpv6Rules(List<SecurityGroupRuleTO> ipv6Rules) {
        this.ipv6Rules = ipv6Rules;
    }
}
