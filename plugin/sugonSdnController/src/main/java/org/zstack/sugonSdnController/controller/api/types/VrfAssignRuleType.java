//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VrfAssignRuleType extends ApiPropertyBase {
    MatchConditionType match_condition;
    Integer vlan_tag;
    String routing_instance;
    Boolean ignore_acl;
    public VrfAssignRuleType() {
    }
    public VrfAssignRuleType(MatchConditionType match_condition, Integer vlan_tag, String routing_instance, Boolean ignore_acl) {
        this.match_condition = match_condition;
        this.vlan_tag = vlan_tag;
        this.routing_instance = routing_instance;
        this.ignore_acl = ignore_acl;
    }
    public VrfAssignRuleType(MatchConditionType match_condition) {
        this(match_condition, null, null, null);    }
    public VrfAssignRuleType(MatchConditionType match_condition, Integer vlan_tag) {
        this(match_condition, vlan_tag, null, null);    }
    public VrfAssignRuleType(MatchConditionType match_condition, Integer vlan_tag, String routing_instance) {
        this(match_condition, vlan_tag, routing_instance, null);    }
    
    public MatchConditionType getMatchCondition() {
        return match_condition;
    }
    
    public void setMatchCondition(MatchConditionType match_condition) {
        this.match_condition = match_condition;
    }
    
    
    public Integer getVlanTag() {
        return vlan_tag;
    }
    
    public void setVlanTag(Integer vlan_tag) {
        this.vlan_tag = vlan_tag;
    }
    
    
    public String getRoutingInstance() {
        return routing_instance;
    }
    
    public void setRoutingInstance(String routing_instance) {
        this.routing_instance = routing_instance;
    }
    
    
    public Boolean getIgnoreAcl() {
        return ignore_acl;
    }
    
    public void setIgnoreAcl(Boolean ignore_acl) {
        this.ignore_acl = ignore_acl;
    }
    
}
