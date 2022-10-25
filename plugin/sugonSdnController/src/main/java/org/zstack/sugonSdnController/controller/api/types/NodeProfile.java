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

public class NodeProfile extends ApiObjectBase {
    private String node_profile_type;
    private String node_profile_vendor;
    private String node_profile_device_family;
    private Boolean node_profile_hitless_upgrade;
    private NodeProfileRolesType node_profile_roles;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> job_template_refs;
    private List<ObjectReference<ApiPropertyBase>> hardware_refs;
    private List<ObjectReference<ApiPropertyBase>> role_definition_refs;
    private List<ObjectReference<ApiPropertyBase>> role_configs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<SerialNumListType>> fabric_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> node_back_refs;

    @Override
    public String getObjectType() {
        return "node-profile";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config");
    }

    @Override
    public String getDefaultParentType() {
        return "global-system-config";
    }

    public void setParent(GlobalSystemConfig parent) {
        super.setParent(parent);
    }
    
    public String getType() {
        return node_profile_type;
    }
    
    public void setType(String node_profile_type) {
        this.node_profile_type = node_profile_type;
    }
    
    
    public String getVendor() {
        return node_profile_vendor;
    }
    
    public void setVendor(String node_profile_vendor) {
        this.node_profile_vendor = node_profile_vendor;
    }
    
    
    public String getDeviceFamily() {
        return node_profile_device_family;
    }
    
    public void setDeviceFamily(String node_profile_device_family) {
        this.node_profile_device_family = node_profile_device_family;
    }
    
    
    public Boolean getHitlessUpgrade() {
        return node_profile_hitless_upgrade;
    }
    
    public void setHitlessUpgrade(Boolean node_profile_hitless_upgrade) {
        this.node_profile_hitless_upgrade = node_profile_hitless_upgrade;
    }
    
    
    public NodeProfileRolesType getRoles() {
        return node_profile_roles;
    }
    
    public void setRoles(NodeProfileRolesType node_profile_roles) {
        this.node_profile_roles = node_profile_roles;
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
    

    public List<ObjectReference<ApiPropertyBase>> getJobTemplate() {
        return job_template_refs;
    }

    public void setJobTemplate(JobTemplate obj) {
        job_template_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        job_template_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addJobTemplate(JobTemplate obj) {
        if (job_template_refs == null) {
            job_template_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        job_template_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeJobTemplate(JobTemplate obj) {
        if (job_template_refs != null) {
            job_template_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearJobTemplate() {
        if (job_template_refs != null) {
            job_template_refs.clear();
            return;
        }
        job_template_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getHardware() {
        return hardware_refs;
    }

    public void setHardware(Hardware obj) {
        hardware_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        hardware_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addHardware(Hardware obj) {
        if (hardware_refs == null) {
            hardware_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        hardware_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeHardware(Hardware obj) {
        if (hardware_refs != null) {
            hardware_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearHardware() {
        if (hardware_refs != null) {
            hardware_refs.clear();
            return;
        }
        hardware_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoleDefinition() {
        return role_definition_refs;
    }

    public void setRoleDefinition(RoleDefinition obj) {
        role_definition_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        role_definition_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addRoleDefinition(RoleDefinition obj) {
        if (role_definition_refs == null) {
            role_definition_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        role_definition_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeRoleDefinition(RoleDefinition obj) {
        if (role_definition_refs != null) {
            role_definition_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearRoleDefinition() {
        if (role_definition_refs != null) {
            role_definition_refs.clear();
            return;
        }
        role_definition_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoleConfigs() {
        return role_configs;
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

    public List<ObjectReference<SerialNumListType>> getFabricBackRefs() {
        return fabric_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodeBackRefs() {
        return node_back_refs;
    }
}