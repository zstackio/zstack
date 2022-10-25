//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RbacRuleType extends ApiPropertyBase {
    String rule_object;
    String rule_field;
    List<RbacPermType> rule_perms;
    public RbacRuleType() {
    }
    public RbacRuleType(String rule_object, String rule_field, List<RbacPermType> rule_perms) {
        this.rule_object = rule_object;
        this.rule_field = rule_field;
        this.rule_perms = rule_perms;
    }
    public RbacRuleType(String rule_object) {
        this(rule_object, null, null);    }
    public RbacRuleType(String rule_object, String rule_field) {
        this(rule_object, rule_field, null);    }
    
    public String getRuleObject() {
        return rule_object;
    }
    
    public void setRuleObject(String rule_object) {
        this.rule_object = rule_object;
    }
    
    
    public String getRuleField() {
        return rule_field;
    }
    
    public void setRuleField(String rule_field) {
        this.rule_field = rule_field;
    }
    
    
    public List<RbacPermType> getRulePerms() {
        return rule_perms;
    }
    
    
    public void addRulePerms(RbacPermType obj) {
        if (rule_perms == null) {
            rule_perms = new ArrayList<RbacPermType>();
        }
        rule_perms.add(obj);
    }
    public void clearRulePerms() {
        rule_perms = null;
    }
    
    
    public void addRulePerms(String role_name, String role_crud) {
        if (rule_perms == null) {
            rule_perms = new ArrayList<RbacPermType>();
        }
        rule_perms.add(new RbacPermType(role_name, role_crud));
    }
    
}
