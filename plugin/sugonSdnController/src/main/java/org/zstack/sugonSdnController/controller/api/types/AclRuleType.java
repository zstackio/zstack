//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AclRuleType extends ApiPropertyBase {
    MatchConditionType match_condition;
    ActionListType action_list;
    String rule_uuid;
    String direction;
    public AclRuleType() {
    }
    public AclRuleType(MatchConditionType match_condition, ActionListType action_list, String rule_uuid, String direction) {
        this.match_condition = match_condition;
        this.action_list = action_list;
        this.rule_uuid = rule_uuid;
        this.direction = direction;
    }
    public AclRuleType(MatchConditionType match_condition) {
        this(match_condition, null, null, null);    }
    public AclRuleType(MatchConditionType match_condition, ActionListType action_list) {
        this(match_condition, action_list, null, null);    }
    public AclRuleType(MatchConditionType match_condition, ActionListType action_list, String rule_uuid) {
        this(match_condition, action_list, rule_uuid, null);    }
    
    public MatchConditionType getMatchCondition() {
        return match_condition;
    }
    
    public void setMatchCondition(MatchConditionType match_condition) {
        this.match_condition = match_condition;
    }
    
    
    public ActionListType getActionList() {
        return action_list;
    }
    
    public void setActionList(ActionListType action_list) {
        this.action_list = action_list;
    }
    
    
    public String getRuleUuid() {
        return rule_uuid;
    }
    
    public void setRuleUuid(String rule_uuid) {
        this.rule_uuid = rule_uuid;
    }
    
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
}
