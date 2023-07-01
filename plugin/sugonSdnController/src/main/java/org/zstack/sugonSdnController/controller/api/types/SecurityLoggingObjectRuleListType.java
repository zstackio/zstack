//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SecurityLoggingObjectRuleListType extends ApiPropertyBase {
    List<SecurityLoggingObjectRuleEntryType> rule;
    public SecurityLoggingObjectRuleListType() {
    }
    public SecurityLoggingObjectRuleListType(List<SecurityLoggingObjectRuleEntryType> rule) {
        this.rule = rule;
    }
    
    public List<SecurityLoggingObjectRuleEntryType> getRule() {
        return rule;
    }
    
    
    public void addRule(SecurityLoggingObjectRuleEntryType obj) {
        if (rule == null) {
            rule = new ArrayList<SecurityLoggingObjectRuleEntryType>();
        }
        rule.add(obj);
    }
    public void clearRule() {
        rule = null;
    }
    
    
    public void addRule(String rule_uuid, Integer rate) {
        if (rule == null) {
            rule = new ArrayList<SecurityLoggingObjectRuleEntryType>();
        }
        rule.add(new SecurityLoggingObjectRuleEntryType(rule_uuid, rate));
    }
    
}
