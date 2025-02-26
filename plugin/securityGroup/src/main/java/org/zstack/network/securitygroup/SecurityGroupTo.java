package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.List;

public class SecurityGroupTo {
    public static final String ACTION_CODE_APPLY_CHAIN = "applyChain";
    public static final String ACTION_CODE_DELETE_CHAIN = "deleteChain";
    private String securityGroupUuid;
    private String securityGroupName;
    private List<String> securityGroupVmIps = new ArrayList<>();
    private List<String> securityGroupVmIp6s = new ArrayList<>();
    private String actionCode;
    private List<RuleTO> rules = new ArrayList<>();

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public List<String> getSecurityGroupVmIps() {
        return securityGroupVmIps;
    }

    public void setSecurityGroupVmIps(List<String> securityGroupVmIps) {
        this.securityGroupVmIps = securityGroupVmIps;
    }

    public List<String> getSecurityGroupVmIp6s() {
        return securityGroupVmIp6s;
    }

    public void setSecurityGroupVmIp6s(List<String> securityGroupVmIp6s) {
        this.securityGroupVmIp6s = securityGroupVmIp6s;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public List<RuleTO> getRules() {
        return rules;
    }

    public void setRules(List<RuleTO> rules) {
        this.rules = rules;
    }
}
