//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VrfAssignTableType extends ApiPropertyBase {
    List<VrfAssignRuleType> vrf_assign_rule;
    public VrfAssignTableType() {
    }
    public VrfAssignTableType(List<VrfAssignRuleType> vrf_assign_rule) {
        this.vrf_assign_rule = vrf_assign_rule;
    }
    
    public List<VrfAssignRuleType> getVrfAssignRule() {
        return vrf_assign_rule;
    }
    
    
    public void addVrfAssignRule(VrfAssignRuleType obj) {
        if (vrf_assign_rule == null) {
            vrf_assign_rule = new ArrayList<VrfAssignRuleType>();
        }
        vrf_assign_rule.add(obj);
    }
    public void clearVrfAssignRule() {
        vrf_assign_rule = null;
    }
    
    
    public void addVrfAssignRule(MatchConditionType match_condition, Integer vlan_tag, String routing_instance, Boolean ignore_acl) {
        if (vrf_assign_rule == null) {
            vrf_assign_rule = new ArrayList<VrfAssignRuleType>();
        }
        vrf_assign_rule.add(new VrfAssignRuleType(match_condition, vlan_tag, routing_instance, ignore_acl));
    }
    
}
