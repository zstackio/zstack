//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class VirtualPortGroup extends ApiObjectBase {
    private Boolean virtual_port_group_lacp_enabled;
    private String virtual_port_group_trunk_port_id;
    private Boolean virtual_port_group_user_created;
    private String virtual_port_group_type;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<VpgInterfaceParametersType>> physical_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> security_group_refs;
    private List<ObjectReference<ApiPropertyBase>> port_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<VMIVirtualPortGroupAttributes>> virtual_machine_interface_back_refs;

    @Override
    public String getObjectType() {
        return "virtual-port-group";
    }

    @Override
    public List<String> getDefaultParent() {
        return null;
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }

    public void setParent(Fabric parent) {
        super.setParent(parent);
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public Boolean getLacpEnabled() {
        return virtual_port_group_lacp_enabled;
    }
    
    public void setLacpEnabled(Boolean virtual_port_group_lacp_enabled) {
        this.virtual_port_group_lacp_enabled = virtual_port_group_lacp_enabled;
    }
    
    
    public String getTrunkPortId() {
        return virtual_port_group_trunk_port_id;
    }
    
    public void setTrunkPortId(String virtual_port_group_trunk_port_id) {
        this.virtual_port_group_trunk_port_id = virtual_port_group_trunk_port_id;
    }
    
    
    public Boolean getUserCreated() {
        return virtual_port_group_user_created;
    }
    
    public void setUserCreated(Boolean virtual_port_group_user_created) {
        this.virtual_port_group_user_created = virtual_port_group_user_created;
    }
    
    
    public String getType() {
        return virtual_port_group_type;
    }
    
    public void setType(String virtual_port_group_type) {
        this.virtual_port_group_type = virtual_port_group_type;
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
    

    public List<ObjectReference<VpgInterfaceParametersType>> getPhysicalInterface() {
        return physical_interface_refs;
    }

    public void setPhysicalInterface(PhysicalInterface obj, VpgInterfaceParametersType data) {
        physical_interface_refs = new ArrayList<ObjectReference<VpgInterfaceParametersType>>();
        physical_interface_refs.add(new ObjectReference<VpgInterfaceParametersType>(obj.getQualifiedName(), data));
    }

    public void addPhysicalInterface(PhysicalInterface obj, VpgInterfaceParametersType data) {
        if (physical_interface_refs == null) {
            physical_interface_refs = new ArrayList<ObjectReference<VpgInterfaceParametersType>>();
        }
        physical_interface_refs.add(new ObjectReference<VpgInterfaceParametersType>(obj.getQualifiedName(), data));
    }

    public void removePhysicalInterface(PhysicalInterface obj, VpgInterfaceParametersType data) {
        if (physical_interface_refs != null) {
            physical_interface_refs.remove(new ObjectReference<VpgInterfaceParametersType>(obj.getQualifiedName(), data));
        }
    }

    public void clearPhysicalInterface() {
        if (physical_interface_refs != null) {
            physical_interface_refs.clear();
            return;
        }
        physical_interface_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getSecurityGroup() {
        return security_group_refs;
    }

    public void setSecurityGroup(SecurityGroup obj) {
        security_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        security_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addSecurityGroup(SecurityGroup obj) {
        if (security_group_refs == null) {
            security_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        security_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeSecurityGroup(SecurityGroup obj) {
        if (security_group_refs != null) {
            security_group_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearSecurityGroup() {
        if (security_group_refs != null) {
            security_group_refs.clear();
            return;
        }
        security_group_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortProfile() {
        return port_profile_refs;
    }

    public void setPortProfile(PortProfile obj) {
        port_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        port_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPortProfile(PortProfile obj) {
        if (port_profile_refs == null) {
            port_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        port_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePortProfile(PortProfile obj) {
        if (port_profile_refs != null) {
            port_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPortProfile() {
        if (port_profile_refs != null) {
            port_profile_refs.clear();
            return;
        }
        port_profile_refs = null;
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

    public List<ObjectReference<VMIVirtualPortGroupAttributes>> getVirtualMachineInterfaceBackRefs() {
        return virtual_machine_interface_back_refs;
    }
}