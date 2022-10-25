//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PolicyEntriesType extends ApiPropertyBase {
    List<PolicyRuleType> policy_rule;
    public PolicyEntriesType() {
    }
    public PolicyEntriesType(List<PolicyRuleType> policy_rule) {
        this.policy_rule = policy_rule;
    }
    
    public List<PolicyRuleType> getPolicyRule() {
        return policy_rule;
    }
    
    
    public void addPolicyRule(PolicyRuleType obj) {
        if (policy_rule == null) {
            policy_rule = new ArrayList<PolicyRuleType>();
        }
        policy_rule.add(obj);
    }
    public void clearPolicyRule() {
        policy_rule = null;
    }
    
}
