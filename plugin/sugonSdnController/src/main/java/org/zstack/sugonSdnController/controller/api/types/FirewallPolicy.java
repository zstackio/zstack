//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class FirewallPolicy extends ApiObjectBase {
    private String draft_mode_state;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<FirewallSequence>> firewall_rule_refs;
    private List<ObjectReference<SloRateType>> security_logging_object_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<FirewallSequence>> application_policy_set_back_refs;

    @Override
    public String getObjectType() {
        return "firewall-policy";
    }

    @Override
    public List<String> getDefaultParent() {
        return null;
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }

    public void setParent(PolicyManagement parent) {
        super.setParent(parent);
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public String getDraftModeState() {
        return draft_mode_state;
    }
    
    public void setDraftModeState(String draft_mode_state) {
        this.draft_mode_state = draft_mode_state;
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
    

    public List<ObjectReference<FirewallSequence>> getFirewallRule() {
        return firewall_rule_refs;
    }

    public void setFirewallRule(FirewallRule obj, FirewallSequence data) {
        firewall_rule_refs = new ArrayList<ObjectReference<FirewallSequence>>();
        firewall_rule_refs.add(new ObjectReference<FirewallSequence>(obj.getQualifiedName(), data));
    }

    public void addFirewallRule(FirewallRule obj, FirewallSequence data) {
        if (firewall_rule_refs == null) {
            firewall_rule_refs = new ArrayList<ObjectReference<FirewallSequence>>();
        }
        firewall_rule_refs.add(new ObjectReference<FirewallSequence>(obj.getQualifiedName(), data));
    }

    public void removeFirewallRule(FirewallRule obj, FirewallSequence data) {
        if (firewall_rule_refs != null) {
            firewall_rule_refs.remove(new ObjectReference<FirewallSequence>(obj.getQualifiedName(), data));
        }
    }

    public void clearFirewallRule() {
        if (firewall_rule_refs != null) {
            firewall_rule_refs.clear();
            return;
        }
        firewall_rule_refs = null;
    }


    public List<ObjectReference<SloRateType>> getSecurityLoggingObject() {
        return security_logging_object_refs;
    }

    public void setSecurityLoggingObject(SecurityLoggingObject obj, SloRateType data) {
        security_logging_object_refs = new ArrayList<ObjectReference<SloRateType>>();
        security_logging_object_refs.add(new ObjectReference<SloRateType>(obj.getQualifiedName(), data));
    }

    public void addSecurityLoggingObject(SecurityLoggingObject obj, SloRateType data) {
        if (security_logging_object_refs == null) {
            security_logging_object_refs = new ArrayList<ObjectReference<SloRateType>>();
        }
        security_logging_object_refs.add(new ObjectReference<SloRateType>(obj.getQualifiedName(), data));
    }

    public void removeSecurityLoggingObject(SecurityLoggingObject obj, SloRateType data) {
        if (security_logging_object_refs != null) {
            security_logging_object_refs.remove(new ObjectReference<SloRateType>(obj.getQualifiedName(), data));
        }
    }

    public void clearSecurityLoggingObject() {
        if (security_logging_object_refs != null) {
            security_logging_object_refs.clear();
            return;
        }
        security_logging_object_refs = null;
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

    public List<ObjectReference<FirewallSequence>> getApplicationPolicySetBackRefs() {
        return application_policy_set_back_refs;
    }
}