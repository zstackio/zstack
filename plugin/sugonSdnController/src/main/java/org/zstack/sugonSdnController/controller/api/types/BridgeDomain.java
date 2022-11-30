//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Lists;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class BridgeDomain extends ApiObjectBase {
    private Boolean mac_learning_enabled;
    private MACLimitControlType mac_limit_control;
    private MACMoveLimitControlType mac_move_control;
    private Integer mac_aging_time;
    private Integer isid;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<BridgeDomainMembershipType>> virtual_machine_interface_back_refs;

    @Override
    public String getObjectType() {
        return "bridge-domain";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain", "default-project", "default-virtual-network");
    }

    @Override
    public String getDefaultParentType() {
        return "virtual-network";
    }

    public void setParent(VirtualNetwork parent) {
        super.setParent(parent);
    }
    
    public Boolean getMacLearningEnabled() {
        return mac_learning_enabled;
    }
    
    public void setMacLearningEnabled(Boolean mac_learning_enabled) {
        this.mac_learning_enabled = mac_learning_enabled;
    }
    
    
    public MACLimitControlType getMacLimitControl() {
        return mac_limit_control;
    }
    
    public void setMacLimitControl(MACLimitControlType mac_limit_control) {
        this.mac_limit_control = mac_limit_control;
    }
    
    
    public MACMoveLimitControlType getMacMoveControl() {
        return mac_move_control;
    }
    
    public void setMacMoveControl(MACMoveLimitControlType mac_move_control) {
        this.mac_move_control = mac_move_control;
    }
    
    
    public Integer getMacAgingTime() {
        return mac_aging_time;
    }
    
    public void setMacAgingTime(Integer mac_aging_time) {
        this.mac_aging_time = mac_aging_time;
    }
    
    
    public Integer getIsid() {
        return isid;
    }
    
    public void setIsid(Integer isid) {
        this.isid = isid;
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

    public List<ObjectReference<BridgeDomainMembershipType>> getVirtualMachineInterfaceBackRefs() {
        return virtual_machine_interface_back_refs;
    }
}