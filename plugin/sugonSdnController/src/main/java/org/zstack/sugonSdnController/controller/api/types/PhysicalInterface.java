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

public class PhysicalInterface extends ApiObjectBase {
    private String ethernet_segment_identifier;
    private String physical_interface_type;
    private MacAddressesType physical_interface_mac_addresses;
    private String physical_interface_port_id;
    private Boolean physical_interface_flow_control;
    private Boolean physical_interface_lacp_force_up;
    private PortParameters physical_interface_port_params;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> physical_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> logical_interfaces;
    private List<ObjectReference<ApiPropertyBase>> port_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ServiceApplianceInterfaceType>> service_appliance_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_interface_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> link_aggregation_group_back_refs;
    private transient List<ObjectReference<VpgInterfaceParametersType>> virtual_port_group_back_refs;

    @Override
    public String getObjectType() {
        return "physical-interface";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config", "default-physical-router");
    }

    @Override
    public String getDefaultParentType() {
        return "physical-router";
    }

    public void setParent(PhysicalRouter parent) {
        super.setParent(parent);
    }
    
    public String getEthernetSegmentIdentifier() {
        return ethernet_segment_identifier;
    }
    
    public void setEthernetSegmentIdentifier(String ethernet_segment_identifier) {
        this.ethernet_segment_identifier = ethernet_segment_identifier;
    }
    
    
    public String getType() {
        return physical_interface_type;
    }
    
    public void setType(String physical_interface_type) {
        this.physical_interface_type = physical_interface_type;
    }
    
    
    public MacAddressesType getMacAddresses() {
        return physical_interface_mac_addresses;
    }
    
    public void setMacAddresses(MacAddressesType physical_interface_mac_addresses) {
        this.physical_interface_mac_addresses = physical_interface_mac_addresses;
    }
    
    
    public String getPortId() {
        return physical_interface_port_id;
    }
    
    public void setPortId(String physical_interface_port_id) {
        this.physical_interface_port_id = physical_interface_port_id;
    }
    
    
    public Boolean getFlowControl() {
        return physical_interface_flow_control;
    }
    
    public void setFlowControl(Boolean physical_interface_flow_control) {
        this.physical_interface_flow_control = physical_interface_flow_control;
    }
    
    
    public Boolean getLacpForceUp() {
        return physical_interface_lacp_force_up;
    }
    
    public void setLacpForceUp(Boolean physical_interface_lacp_force_up) {
        this.physical_interface_lacp_force_up = physical_interface_lacp_force_up;
    }
    
    
    public PortParameters getPortParams() {
        return physical_interface_port_params;
    }
    
    public void setPortParams(PortParameters physical_interface_port_params) {
        this.physical_interface_port_params = physical_interface_port_params;
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
    

    public List<ObjectReference<ApiPropertyBase>> getPhysicalInterface() {
        return physical_interface_refs;
    }

    public void setPhysicalInterface(PhysicalInterface obj) {
        physical_interface_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        physical_interface_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPhysicalInterface(PhysicalInterface obj) {
        if (physical_interface_refs == null) {
            physical_interface_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        physical_interface_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePhysicalInterface(PhysicalInterface obj) {
        if (physical_interface_refs != null) {
            physical_interface_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPhysicalInterface() {
        if (physical_interface_refs != null) {
            physical_interface_refs.clear();
            return;
        }
        physical_interface_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalInterfaces() {
        return logical_interfaces;
    }

    public List<ObjectReference<ApiPropertyBase>> getPort() {
        return port_refs;
    }

    public void setPort(Port obj) {
        port_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        port_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPort(Port obj) {
        if (port_refs == null) {
            port_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        port_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePort(Port obj) {
        if (port_refs != null) {
            port_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPort() {
        if (port_refs != null) {
            port_refs.clear();
            return;
        }
        port_refs = null;
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

    public List<ObjectReference<ServiceApplianceInterfaceType>> getServiceApplianceBackRefs() {
        return service_appliance_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterfaceBackRefs() {
        return virtual_machine_interface_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalInterfaceBackRefs() {
        return physical_interface_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLinkAggregationGroupBackRefs() {
        return link_aggregation_group_back_refs;
    }

    public List<ObjectReference<VpgInterfaceParametersType>> getVirtualPortGroupBackRefs() {
        return virtual_port_group_back_refs;
    }
}