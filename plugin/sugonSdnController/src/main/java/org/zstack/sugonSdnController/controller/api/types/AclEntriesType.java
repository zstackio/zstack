//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AclEntriesType extends ApiPropertyBase {
    Boolean dynamic;
    List<AclRuleType> acl_rule;
    public AclEntriesType() {
    }
    public AclEntriesType(Boolean dynamic, List<AclRuleType> acl_rule) {
        this.dynamic = dynamic;
        this.acl_rule = acl_rule;
    }
    public AclEntriesType(Boolean dynamic) {
        this(dynamic, null);    }
    
    public Boolean getDynamic() {
        return dynamic;
    }
    
    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }
    
    
    public List<AclRuleType> getAclRule() {
        return acl_rule;
    }
    
    
    public void addAclRule(AclRuleType obj) {
        if (acl_rule == null) {
            acl_rule = new ArrayList<AclRuleType>();
        }
        acl_rule.add(obj);
    }
    public void clearAclRule() {
        acl_rule = null;
    }
    
    
    public void addAclRule(MatchConditionType match_condition, ActionListType action_list, String rule_uuid, String direction) {
        if (acl_rule == null) {
            acl_rule = new ArrayList<AclRuleType>();
        }
        acl_rule.add(new AclRuleType(match_condition, action_list, rule_uuid, direction));
    }
    
}
