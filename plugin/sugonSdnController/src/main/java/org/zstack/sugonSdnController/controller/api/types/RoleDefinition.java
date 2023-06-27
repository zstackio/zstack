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

public class RoleDefinition extends ApiObjectBase {
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> feature_refs;
    private List<ObjectReference<ApiPropertyBase>> physical_role_refs;
    private List<ObjectReference<ApiPropertyBase>> overlay_role_refs;
    private List<ObjectReference<ApiPropertyBase>> feature_configs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> node_profile_back_refs;

    @Override
    public String getObjectType() {
        return "role-definition";
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
    

    public List<ObjectReference<ApiPropertyBase>> getFeature() {
        return feature_refs;
    }

    public void setFeature(Feature obj) {
        feature_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        feature_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addFeature(Feature obj) {
        if (feature_refs == null) {
            feature_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        feature_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeFeature(Feature obj) {
        if (feature_refs != null) {
            feature_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearFeature() {
        if (feature_refs != null) {
            feature_refs.clear();
            return;
        }
        feature_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRole() {
        return physical_role_refs;
    }

    public void setPhysicalRole(PhysicalRole obj) {
        physical_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        physical_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPhysicalRole(PhysicalRole obj) {
        if (physical_role_refs == null) {
            physical_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        physical_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePhysicalRole(PhysicalRole obj) {
        if (physical_role_refs != null) {
            physical_role_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPhysicalRole() {
        if (physical_role_refs != null) {
            physical_role_refs.clear();
            return;
        }
        physical_role_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getOverlayRole() {
        return overlay_role_refs;
    }

    public void setOverlayRole(OverlayRole obj) {
        overlay_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        overlay_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addOverlayRole(OverlayRole obj) {
        if (overlay_role_refs == null) {
            overlay_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        overlay_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeOverlayRole(OverlayRole obj) {
        if (overlay_role_refs != null) {
            overlay_role_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearOverlayRole() {
        if (overlay_role_refs != null) {
            overlay_role_refs.clear();
            return;
        }
        overlay_role_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getFeatureConfigs() {
        return feature_configs;
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

    public List<ObjectReference<ApiPropertyBase>> getNodeProfileBackRefs() {
        return node_profile_back_refs;
    }
}