//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class ApplicationPolicySet extends ApiObjectBase {
    private String draft_mode_state;
    private Boolean all_applications;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<FirewallSequence>> firewall_policy_refs;
    private List<ObjectReference<ApiPropertyBase>> global_vrouter_config_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> project_back_refs;

    @Override
    public String getObjectType() {
        return "application-policy-set";
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
    
    
    public Boolean getAllApplications() {
        return all_applications;
    }
    
    public void setAllApplications(Boolean all_applications) {
        this.all_applications = all_applications;
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
    

    public List<ObjectReference<FirewallSequence>> getFirewallPolicy() {
        return firewall_policy_refs;
    }

    public void setFirewallPolicy(FirewallPolicy obj, FirewallSequence data) {
        firewall_policy_refs = new ArrayList<ObjectReference<FirewallSequence>>();
        firewall_policy_refs.add(new ObjectReference<FirewallSequence>(obj.getQualifiedName(), data));
    }

    public void addFirewallPolicy(FirewallPolicy obj, FirewallSequence data) {
        if (firewall_policy_refs == null) {
            firewall_policy_refs = new ArrayList<ObjectReference<FirewallSequence>>();
        }
        firewall_policy_refs.add(new ObjectReference<FirewallSequence>(obj.getQualifiedName(), data));
    }

    public void removeFirewallPolicy(FirewallPolicy obj, FirewallSequence data) {
        if (firewall_policy_refs != null) {
            firewall_policy_refs.remove(new ObjectReference<FirewallSequence>(obj.getQualifiedName(), data));
        }
    }

    public void clearFirewallPolicy() {
        if (firewall_policy_refs != null) {
            firewall_policy_refs.clear();
            return;
        }
        firewall_policy_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getGlobalVrouterConfig() {
        return global_vrouter_config_refs;
    }

    public void setGlobalVrouterConfig(GlobalVrouterConfig obj) {
        global_vrouter_config_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        global_vrouter_config_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addGlobalVrouterConfig(GlobalVrouterConfig obj) {
        if (global_vrouter_config_refs == null) {
            global_vrouter_config_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        global_vrouter_config_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeGlobalVrouterConfig(GlobalVrouterConfig obj) {
        if (global_vrouter_config_refs != null) {
            global_vrouter_config_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearGlobalVrouterConfig() {
        if (global_vrouter_config_refs != null) {
            global_vrouter_config_refs.clear();
            return;
        }
        global_vrouter_config_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getProjectBackRefs() {
        return project_back_refs;
    }
}