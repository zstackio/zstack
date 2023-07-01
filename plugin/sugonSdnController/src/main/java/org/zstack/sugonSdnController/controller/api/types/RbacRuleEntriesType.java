//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RbacRuleEntriesType extends ApiPropertyBase {
    List<RbacRuleType> rbac_rule;
    public RbacRuleEntriesType() {
    }
    public RbacRuleEntriesType(List<RbacRuleType> rbac_rule) {
        this.rbac_rule = rbac_rule;
    }
    
    public List<RbacRuleType> getRbacRule() {
        return rbac_rule;
    }
    
    
    public void addRbacRule(RbacRuleType obj) {
        if (rbac_rule == null) {
            rbac_rule = new ArrayList<RbacRuleType>();
        }
        rbac_rule.add(obj);
    }
    public void clearRbacRule() {
        rbac_rule = null;
    }
    
    
    public void addRbacRule(String rule_object, String rule_field, List<RbacPermType> rule_perms) {
        if (rbac_rule == null) {
            rbac_rule = new ArrayList<RbacRuleType>();
        }
        rbac_rule.add(new RbacRuleType(rule_object, rule_field, rule_perms));
    }
    
}
