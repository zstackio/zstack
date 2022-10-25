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

public class VirtualIp extends ApiObjectBase {
    private VirtualIpType virtual_ip_properties;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_pool_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "virtual-ip";
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
    
    public VirtualIpType getProperties() {
        return virtual_ip_properties;
    }
    
    public void setProperties(VirtualIpType virtual_ip_properties) {
        this.virtual_ip_properties = virtual_ip_properties;
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
    

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerPool() {
        return loadbalancer_pool_refs;
    }

    public void setLoadbalancerPool(LoadbalancerPool obj) {
        loadbalancer_pool_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        loadbalancer_pool_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addLoadbalancerPool(LoadbalancerPool obj) {
        if (loadbalancer_pool_refs == null) {
            loadbalancer_pool_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        loadbalancer_pool_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeLoadbalancerPool(LoadbalancerPool obj) {
        if (loadbalancer_pool_refs != null) {
            loadbalancer_pool_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearLoadbalancerPool() {
        if (loadbalancer_pool_refs != null) {
            loadbalancer_pool_refs.clear();
            return;
        }
        loadbalancer_pool_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterface() {
        return virtual_machine_interface_refs;
    }

    public void setVirtualMachineInterface(VirtualMachineInterface obj) {
        virtual_machine_interface_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_machine_interface_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualMachineInterface(VirtualMachineInterface obj) {
        if (virtual_machine_interface_refs == null) {
            virtual_machine_interface_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_machine_interface_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualMachineInterface(VirtualMachineInterface obj) {
        if (virtual_machine_interface_refs != null) {
            virtual_machine_interface_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualMachineInterface() {
        if (virtual_machine_interface_refs != null) {
            virtual_machine_interface_refs.clear();
            return;
        }
        virtual_machine_interface_refs = null;
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