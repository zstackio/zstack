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

public class ServiceAppliance extends ApiObjectBase {
    private UserCredentials service_appliance_user_credentials;
    private String service_appliance_ip_address;
    private String service_appliance_virtualization_type;
    private KeyValuePairs service_appliance_properties;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ServiceApplianceInterfaceType>> physical_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "service-appliance";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config", "default-service-appliance-set");
    }

    @Override
    public String getDefaultParentType() {
        return "service-appliance-set";
    }

    public void setParent(ServiceApplianceSet parent) {
        super.setParent(parent);
    }
    
    public UserCredentials getUserCredentials() {
        return service_appliance_user_credentials;
    }
    
    public void setUserCredentials(UserCredentials service_appliance_user_credentials) {
        this.service_appliance_user_credentials = service_appliance_user_credentials;
    }
    
    
    public String getIpAddress() {
        return service_appliance_ip_address;
    }
    
    public void setIpAddress(String service_appliance_ip_address) {
        this.service_appliance_ip_address = service_appliance_ip_address;
    }
    
    
    public String getVirtualizationType() {
        return service_appliance_virtualization_type;
    }
    
    public void setVirtualizationType(String service_appliance_virtualization_type) {
        this.service_appliance_virtualization_type = service_appliance_virtualization_type;
    }
    
    
    public KeyValuePairs getProperties() {
        return service_appliance_properties;
    }
    
    public void setProperties(KeyValuePairs service_appliance_properties) {
        this.service_appliance_properties = service_appliance_properties;
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
    

    public List<ObjectReference<ServiceApplianceInterfaceType>> getPhysicalInterface() {
        return physical_interface_refs;
    }

    public void setPhysicalInterface(PhysicalInterface obj, ServiceApplianceInterfaceType data) {
        physical_interface_refs = new ArrayList<ObjectReference<ServiceApplianceInterfaceType>>();
        physical_interface_refs.add(new ObjectReference<ServiceApplianceInterfaceType>(obj.getQualifiedName(), data));
    }

    public void addPhysicalInterface(PhysicalInterface obj, ServiceApplianceInterfaceType data) {
        if (physical_interface_refs == null) {
            physical_interface_refs = new ArrayList<ObjectReference<ServiceApplianceInterfaceType>>();
        }
        physical_interface_refs.add(new ObjectReference<ServiceApplianceInterfaceType>(obj.getQualifiedName(), data));
    }

    public void removePhysicalInterface(PhysicalInterface obj, ServiceApplianceInterfaceType data) {
        if (physical_interface_refs != null) {
            physical_interface_refs.remove(new ObjectReference<ServiceApplianceInterfaceType>(obj.getQualifiedName(), data));
        }
    }

    public void clearPhysicalInterface() {
        if (physical_interface_refs != null) {
            physical_interface_refs.clear();
            return;
        }
        physical_interface_refs = null;
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