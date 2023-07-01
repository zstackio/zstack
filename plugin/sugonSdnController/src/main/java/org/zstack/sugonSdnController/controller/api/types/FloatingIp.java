//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class FloatingIp extends ApiObjectBase {
    private String floating_ip_address;
    private Boolean floating_ip_is_virtual_ip;
    private String floating_ip_fixed_ip_address;
    private String floating_ip_address_family;
    private Boolean floating_ip_port_mappings_enable;
    private PortMappings floating_ip_port_mappings;
    private String floating_ip_traffic_direction;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> project_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> customer_attachment_back_refs;

    @Override
    public String getObjectType() {
        return "floating-ip";
    }

    @Override
    public List<String> getDefaultParent() {
        return null;
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }

    public void setParent(FloatingIpPool parent) {
        super.setParent(parent);
    }

    public void setParent(InstanceIp parent) {
        super.setParent(parent);
    }
    
    public String getAddress() {
        return floating_ip_address;
    }
    
    public void setAddress(String floating_ip_address) {
        this.floating_ip_address = floating_ip_address;
    }
    
    
    public Boolean getIsVirtualIp() {
        return floating_ip_is_virtual_ip;
    }
    
    public void setIsVirtualIp(Boolean floating_ip_is_virtual_ip) {
        this.floating_ip_is_virtual_ip = floating_ip_is_virtual_ip;
    }
    
    
    public String getFixedIpAddress() {
        return floating_ip_fixed_ip_address;
    }
    
    public void setFixedIpAddress(String floating_ip_fixed_ip_address) {
        this.floating_ip_fixed_ip_address = floating_ip_fixed_ip_address;
    }
    
    
    public String getAddressFamily() {
        return floating_ip_address_family;
    }
    
    public void setAddressFamily(String floating_ip_address_family) {
        this.floating_ip_address_family = floating_ip_address_family;
    }
    
    
    public Boolean getPortMappingsEnable() {
        return floating_ip_port_mappings_enable;
    }
    
    public void setPortMappingsEnable(Boolean floating_ip_port_mappings_enable) {
        this.floating_ip_port_mappings_enable = floating_ip_port_mappings_enable;
    }
    
    
    public PortMappings getPortMappings() {
        return floating_ip_port_mappings;
    }
    
    public void setPortMappings(PortMappings floating_ip_port_mappings) {
        this.floating_ip_port_mappings = floating_ip_port_mappings;
    }
    
    
    public String getTrafficDirection() {
        return floating_ip_traffic_direction;
    }
    
    public void setTrafficDirection(String floating_ip_traffic_direction) {
        this.floating_ip_traffic_direction = floating_ip_traffic_direction;
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
    

    public List<ObjectReference<ApiPropertyBase>> getProject() {
        return project_refs;
    }

    public void setProject(Project obj) {
        project_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        project_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addProject(Project obj) {
        if (project_refs == null) {
            project_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        project_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeProject(Project obj) {
        if (project_refs != null) {
            project_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearProject() {
        if (project_refs != null) {
            project_refs.clear();
            return;
        }
        project_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getCustomerAttachmentBackRefs() {
        return customer_attachment_back_refs;
    }
}