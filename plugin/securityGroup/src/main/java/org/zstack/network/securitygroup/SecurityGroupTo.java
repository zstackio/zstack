package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.List;

public class SecurityGroupTo {
    public static final String ACTION_CODE_APPLY_CHAIN = "applyChain";
    public static final String ACTION_CODE_DELETE_CHAIN = "deleteChain";
    private String SecurityGroupUuid;
    private String SecurityGroupName;
    private List<String> SecurityGroupVmIps = new ArrayList<>();
    private List<String> SecurityGroupVmIp6s = new ArrayList<>();
    private String actionCode;
    private List<RuleTO> rules = new ArrayList<>();

    public String getSecurityGroupUuid() {
        return SecurityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        SecurityGroupUuid = securityGroupUuid;
    }

    public String getSecurityGroupName() {
        return SecurityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        SecurityGroupName = securityGroupName;
    }

    public List<String> getSecurityGroupVmIps() {
        return SecurityGroupVmIps;
    }

    public void setSecurityGroupVmIps(List<String> securityGroupVmIps) {
        SecurityGroupVmIps = securityGroupVmIps;
    }

    public List<String> getSecurityGroupVmIp6s() {
        return SecurityGroupVmIp6s;
    }

    public void setSecurityGroupVmIp6s(List<String> securityGroupVmIp6s) {
        SecurityGroupVmIp6s = securityGroupVmIp6s;
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
