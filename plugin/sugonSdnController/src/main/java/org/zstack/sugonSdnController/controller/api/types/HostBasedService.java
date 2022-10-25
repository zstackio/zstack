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

public class HostBasedService extends ApiObjectBase {
    private String host_based_service_type;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ServiceVirtualNetworkType>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "host-based-service";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain", "default-project");
    }

    @Override
    public String getDefaultParentType() {
        return "project";
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public String getType() {
        return host_based_service_type;
    }
    
    public void setType(String host_based_service_type) {
        this.host_based_service_type = host_based_service_type;
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
    

    public List<ObjectReference<ServiceVirtualNetworkType>> getVirtualNetwork() {
        return virtual_network_refs;
    }

    public void setVirtualNetwork(VirtualNetwork obj, ServiceVirtualNetworkType data) {
        virtual_network_refs = new ArrayList<ObjectReference<ServiceVirtualNetworkType>>();
        virtual_network_refs.add(new ObjectReference<ServiceVirtualNetworkType>(obj.getQualifiedName(), data));
    }

    public void addVirtualNetwork(VirtualNetwork obj, ServiceVirtualNetworkType data) {
        if (virtual_network_refs == null) {
            virtual_network_refs = new ArrayList<ObjectReference<ServiceVirtualNetworkType>>();
        }
        virtual_network_refs.add(new ObjectReference<ServiceVirtualNetworkType>(obj.getQualifiedName(), data));
    }

    public void removeVirtualNetwork(VirtualNetwork obj, ServiceVirtualNetworkType data) {
        if (virtual_network_refs != null) {
            virtual_network_refs.remove(new ObjectReference<ServiceVirtualNetworkType>(obj.getQualifiedName(), data));
        }
    }

    public void clearVirtualNetwork() {
        if (virtual_network_refs != null) {
            virtual_network_refs.clear();
            return;
        }
        virtual_network_refs = null;
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