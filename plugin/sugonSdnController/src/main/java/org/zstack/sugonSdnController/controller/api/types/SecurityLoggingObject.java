//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class SecurityLoggingObject extends ApiObjectBase {
    private SecurityLoggingObjectRuleListType security_logging_object_rules;
    private Integer security_logging_object_rate;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<SecurityLoggingObjectRuleListType>> network_policy_refs;
    private List<ObjectReference<SecurityLoggingObjectRuleListType>> security_group_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_network_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_back_refs;
    private transient List<ObjectReference<SloRateType>> firewall_policy_back_refs;
    private transient List<ObjectReference<SloRateType>> firewall_rule_back_refs;

    @Override
    public String getObjectType() {
        return "security-logging-object";
    }

    @Override
    public List<String> getDefaultParent() {
        return null;
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }

    public void setParent(GlobalVrouterConfig parent) {
        super.setParent(parent);
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public SecurityLoggingObjectRuleListType getRules() {
        return security_logging_object_rules;
    }
    
    public void setRules(SecurityLoggingObjectRuleListType security_logging_object_rules) {
        this.security_logging_object_rules = security_logging_object_rules;
    }
    
    
    public Integer getRate() {
        return security_logging_object_rate;
    }
    
    public void setRate(Integer security_logging_object_rate) {
        this.security_logging_object_rate = security_logging_object_rate;
    }
    
    
    public IdPermsType getIdPerms() {
        return id_perms;
    }
    
    public void setIdPerms(IdPermsType id_perms) {
        this.id_perms = id_perms;
    }
    
    
    public PermType2 getPerms2() {
        return perms2;
    }
    
    public void setPerms2(PermType2 perms2) {
        this.perms2 = perms2;
    }
    
    
    public KeyValuePairs getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(KeyValuePairs annotations) {
        this.annotations = annotations;
    }
    
    
    public String getDisplayName() {
        return display_name;
    }
    
    public void setDisplayName(String display_name) {
        this.display_name = display_name;
    }
    

    public List<ObjectReference<SecurityLoggingObjectRuleListType>> getNetworkPolicy() {
        return network_policy_refs;
    }

    public void setNetworkPolicy(NetworkPolicy obj, SecurityLoggingObjectRuleListType data) {
        network_policy_refs = new ArrayList<ObjectReference<SecurityLoggingObjectRuleListType>>();
        network_policy_refs.add(new ObjectReference<SecurityLoggingObjectRuleListType>(obj.getQualifiedName(), data));
    }

    public void addNetworkPolicy(NetworkPolicy obj, SecurityLoggingObjectRuleListType data) {
        if (network_policy_refs == null) {
            network_policy_refs = new ArrayList<ObjectReference<SecurityLoggingObjectRuleListType>>();
        }
        network_policy_refs.add(new ObjectReference<SecurityLoggingObjectRuleListType>(obj.getQualifiedName(), data));
    }

    public void removeNetworkPolicy(NetworkPolicy obj, SecurityLoggingObjectRuleListType data) {
        if (network_policy_refs != null) {
            network_policy_refs.remove(new ObjectReference<SecurityLoggingObjectRuleListType>(obj.getQualifiedName(), data));
        }
    }

    public void clearNetworkPolicy() {
        if (network_policy_refs != null) {
            network_policy_refs.clear();
            return;
        }
        network_policy_refs = null;
    }


    public List<ObjectReference<SecurityLoggingObjectRuleListType>> getSecurityGroup() {
        return security_group_refs;
    }

    public void setSecurityGroup(SecurityGroup obj, SecurityLoggingObjectRuleListType data) {
        security_group_refs = new ArrayList<ObjectReference<SecurityLoggingObjectRuleListType>>();
        security_group_refs.add(new ObjectReference<SecurityLoggingObjectRuleListType>(obj.getQualifiedName(), data));
    }

    public void addSecurityGroup(SecurityGroup obj, SecurityLoggingObjectRuleListType data) {
        if (security_group_refs == null) {
            security_group_refs = new ArrayList<ObjectReference<SecurityLoggingObjectRuleListType>>();
        }
        security_group_refs.add(new ObjectReference<SecurityLoggingObjectRuleListType>(obj.getQualifiedName(), data));
    }

    public void removeSecurityGroup(SecurityGroup obj, SecurityLoggingObjectRuleListType data) {
        if (security_group_refs != null) {
            security_group_refs.remove(new ObjectReference<SecurityLoggingObjectRuleListType>(obj.getQualifiedName(), data));
        }
    }

    public void clearSecurityGroup() {
        if (security_group_refs != null) {
            security_group_refs.clear();
            return;
        }
        security_group_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getTag() {
        return tag_refs;
    }

    public void setTag(Tag obj) {
        tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addTag(Tag obj) {
        if (tag_refs == null) {
            tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeTag(Tag obj) {
        if (tag_refs != null) {
            tag_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearTag() {
        if (tag_refs != null) {
            tag_refs.clear();
            return;
        }
        tag_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetworkBackRefs() {
        return virtual_network_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterfaceBackRefs() {
        return virtual_machine_interface_back_refs;
    }

    public List<ObjectReference<SloRateType>> getFirewallPolicyBackRefs() {
        return firewall_policy_back_refs;
    }

    public List<ObjectReference<SloRateType>> getFirewallRuleBackRefs() {
        return firewall_rule_back_refs;
    }
}