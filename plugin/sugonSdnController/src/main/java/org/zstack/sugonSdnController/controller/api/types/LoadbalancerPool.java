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

public class LoadbalancerPool extends ApiObjectBase {
    private LoadbalancerPoolType loadbalancer_pool_properties;
    private String loadbalancer_pool_provider;
    private KeyValuePairs loadbalancer_pool_custom_attributes;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> service_instance_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_listener_refs;
    private List<ObjectReference<ApiPropertyBase>> service_appliance_set_refs;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_members;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_healthmonitor_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_ip_back_refs;

    @Override
    public String getObjectType() {
        return "loadbalancer-pool";
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
    
    public LoadbalancerPoolType getProperties() {
        return loadbalancer_pool_properties;
    }
    
    public void setProperties(LoadbalancerPoolType loadbalancer_pool_properties) {
        this.loadbalancer_pool_properties = loadbalancer_pool_properties;
    }
    
    
    public String getProvider() {
        return loadbalancer_pool_provider;
    }
    
    public void setProvider(String loadbalancer_pool_provider) {
        this.loadbalancer_pool_provider = loadbalancer_pool_provider;
    }
    
    
    public KeyValuePairs getCustomAttributes() {
        return loadbalancer_pool_custom_attributes;
    }
    
    public void setCustomAttributes(KeyValuePairs loadbalancer_pool_custom_attributes) {
        this.loadbalancer_pool_custom_attributes = loadbalancer_pool_custom_attributes;
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
    

    public List<ObjectReference<ApiPropertyBase>> getServiceInstance() {
        return service_instance_refs;
    }

    public void setServiceInstance(ServiceInstance obj) {
        service_instance_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        service_instance_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addServiceInstance(ServiceInstance obj) {
        if (service_instance_refs == null) {
            service_instance_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        service_instance_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeServiceInstance(ServiceInstance obj) {
        if (service_instance_refs != null) {
            service_instance_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearServiceInstance() {
        if (service_instance_refs != null) {
            service_instance_refs.clear();
            return;
        }
        service_instance_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerListener() {
        return loadbalancer_listener_refs;
    }

    public void setLoadbalancerListener(LoadbalancerListener obj) {
        loadbalancer_listener_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        loadbalancer_listener_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addLoadbalancerListener(LoadbalancerListener obj) {
        if (loadbalancer_listener_refs == null) {
            loadbalancer_listener_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        loadbalancer_listener_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeLoadbalancerListener(LoadbalancerListener obj) {
        if (loadbalancer_listener_refs != null) {
            loadbalancer_listener_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearLoadbalancerListener() {
        if (loadbalancer_listener_refs != null) {
            loadbalancer_listener_refs.clear();
            return;
        }
        loadbalancer_listener_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceApplianceSet() {
        return service_appliance_set_refs;
    }

    public void setServiceApplianceSet(ServiceApplianceSet obj) {
        service_appliance_set_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        service_appliance_set_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addServiceApplianceSet(ServiceApplianceSet obj) {
        if (service_appliance_set_refs == null) {
            service_appliance_set_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        service_appliance_set_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeServiceApplianceSet(ServiceApplianceSet obj) {
        if (service_appliance_set_refs != null) {
            service_appliance_set_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearServiceApplianceSet() {
        if (service_appliance_set_refs != null) {
            service_appliance_set_refs.clear();
            return;
        }
        service_appliance_set_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerMembers() {
        return loadbalancer_members;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerHealthmonitor() {
        return loadbalancer_healthmonitor_refs;
    }

    public void setLoadbalancerHealthmonitor(LoadbalancerHealthmonitor obj) {
        loadbalancer_healthmonitor_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        loadbalancer_healthmonitor_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addLoadbalancerHealthmonitor(LoadbalancerHealthmonitor obj) {
        if (loadbalancer_healthmonitor_refs == null) {
            loadbalancer_healthmonitor_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        loadbalancer_healthmonitor_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeLoadbalancerHealthmonitor(LoadbalancerHealthmonitor obj) {
        if (loadbalancer_healthmonitor_refs != null) {
            loadbalancer_healthmonitor_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearLoadbalancerHealthmonitor() {
        if (loadbalancer_healthmonitor_refs != null) {
            loadbalancer_healthmonitor_refs.clear();
            return;
        }
        loadbalancer_healthmonitor_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getVirtualIpBackRefs() {
        return virtual_ip_back_refs;
    }
}