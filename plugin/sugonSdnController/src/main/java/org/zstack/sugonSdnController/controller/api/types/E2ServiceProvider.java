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

public class E2ServiceProvider extends ApiObjectBase {
    private Boolean e2_service_provider_promiscuous;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> peering_policy_refs;
    private List<ObjectReference<ApiPropertyBase>> physical_router_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "e2-service-provider";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList();
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }
    
    public Boolean getPromiscuous() {
        return e2_service_provider_promiscuous;
    }
    
    public void setPromiscuous(Boolean e2_service_provider_promiscuous) {
        this.e2_service_provider_promiscuous = e2_service_provider_promiscuous;
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
    

    public List<ObjectReference<ApiPropertyBase>> getPeeringPolicy() {
        return peering_policy_refs;
    }

    public void setPeeringPolicy(PeeringPolicy obj) {
        peering_policy_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        peering_policy_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPeeringPolicy(PeeringPolicy obj) {
        if (peering_policy_refs == null) {
            peering_policy_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        peering_policy_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePeeringPolicy(PeeringPolicy obj) {
        if (peering_policy_refs != null) {
            peering_policy_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPeeringPolicy() {
        if (peering_policy_refs != null) {
            peering_policy_refs.clear();
            return;
        }
        peering_policy_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouter() {
        return physical_router_refs;
    }

    public void setPhysicalRouter(PhysicalRouter obj) {
        physical_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        physical_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPhysicalRouter(PhysicalRouter obj) {
        if (physical_router_refs == null) {
            physical_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        physical_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePhysicalRouter(PhysicalRouter obj) {
        if (physical_router_refs != null) {
            physical_router_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPhysicalRouter() {
        if (physical_router_refs != null) {
            physical_router_refs.clear();
            return;
        }
        physical_router_refs = null;
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
}