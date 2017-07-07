package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SecurityGroupRuleTO {
    public static final String ACTION_CODE_APPLY_RULE = "applyRule";
    public static final String ACTION_CODE_DELETE_CHAIN = "deleteChain";

    private String vmNicInternalName;
    private List<RuleTO> rules;
    private String ingressDefaultPolicy;
    private String egressDefaultPolicy;
    private String vmNicUuid;
    private String vmNicMac;
    private String vmNicIp;
    private List<RuleTO> securityGroupBaseRules;
    private String actionCode = ACTION_CODE_APPLY_RULE;

    public String getVmNicMac() {
        return vmNicMac;
    }

    public void setVmNicMac(String vmNicMac) {
        this.vmNicMac = vmNicMac;
    }

    public String getVmNicIp() {
        return vmNicIp;
    }

    public void setVmNicIp(String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getIngressDefaultPolicy() {
        return ingressDefaultPolicy;
    }

    public void setIngressDefaultPolicy(String ingressDefaultPolicy) {
        this.ingressDefaultPolicy = ingressDefaultPolicy;
    }

    public String getEgressDefaultPolicy() {
        return egressDefaultPolicy;
    }

    public void setEgressDefaultPolicy(String egressDefaultPolicy) {
        this.egressDefaultPolicy = egressDefaultPolicy;
    }

    public String getVmNicInternalName() {
        return vmNicInternalName;
    }
    public void setVmNicInternalName(String vmNicInternalName) {
        this.vmNicInternalName = vmNicInternalName;
    }
    public List<RuleTO> getRules() {
        if (rules == null) {
            rules = new ArrayList<RuleTO>();
        }
        return rules;
    }

    public void setRules(List<RuleTO> rules) {
        this.rules = rules;
    }

    public void setSecurityGroupBaseRules(List<RuleTO> securityGroupBaseRules) {
        this.securityGroupBaseRules = securityGroupBaseRules;
    }

    public List<RuleTO> getSecurityGroupBaseRules() {
        return securityGroupBaseRules;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\nvmNicInternalName: %s", vmNicInternalName));
        for (RuleTO to : rules) {
            sb.append(String.format("\n%s", to.toFullString()));
        }
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return vmNicInternalName.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SecurityGroupRuleTO)) {
            return false;
        }

        SecurityGroupRuleTO rto = (SecurityGroupRuleTO)obj;
        return rto.getVmNicInternalName().equals(this.getVmNicInternalName());
    }
}
