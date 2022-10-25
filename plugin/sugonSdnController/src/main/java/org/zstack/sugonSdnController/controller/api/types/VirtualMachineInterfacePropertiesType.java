//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VirtualMachineInterfacePropertiesType extends ApiPropertyBase {
    String service_interface_type;
    InterfaceMirrorType interface_mirror;
    Integer local_preference;
    Integer sub_interface_vlan_tag;
    Integer max_flows;
    public VirtualMachineInterfacePropertiesType() {
    }
    public VirtualMachineInterfacePropertiesType(String service_interface_type, InterfaceMirrorType interface_mirror, Integer local_preference, Integer sub_interface_vlan_tag, Integer max_flows) {
        this.service_interface_type = service_interface_type;
        this.interface_mirror = interface_mirror;
        this.local_preference = local_preference;
        this.sub_interface_vlan_tag = sub_interface_vlan_tag;
        this.max_flows = max_flows;
    }
    public VirtualMachineInterfacePropertiesType(String service_interface_type) {
        this(service_interface_type, null, null, null, 0);    }
    public VirtualMachineInterfacePropertiesType(String service_interface_type, InterfaceMirrorType interface_mirror) {
        this(service_interface_type, interface_mirror, null, null, 0);    }
    public VirtualMachineInterfacePropertiesType(String service_interface_type, InterfaceMirrorType interface_mirror, Integer local_preference) {
        this(service_interface_type, interface_mirror, local_preference, null, 0);    }
    public VirtualMachineInterfacePropertiesType(String service_interface_type, InterfaceMirrorType interface_mirror, Integer local_preference, Integer sub_interface_vlan_tag) {
        this(service_interface_type, interface_mirror, local_preference, sub_interface_vlan_tag, 0);    }
    
    public String getServiceInterfaceType() {
        return service_interface_type;
    }
    
    public void setServiceInterfaceType(String service_interface_type) {
        this.service_interface_type = service_interface_type;
    }
    
    
    public InterfaceMirrorType getInterfaceMirror() {
        return interface_mirror;
    }
    
    public void setInterfaceMirror(InterfaceMirrorType interface_mirror) {
        this.interface_mirror = interface_mirror;
    }
    
    
    public Integer getLocalPreference() {
        return local_preference;
    }
    
    public void setLocalPreference(Integer local_preference) {
        this.local_preference = local_preference;
    }
    
    
    public Integer getSubInterfaceVlanTag() {
        return sub_interface_vlan_tag;
    }
    
    public void setSubInterfaceVlanTag(Integer sub_interface_vlan_tag) {
        this.sub_interface_vlan_tag = sub_interface_vlan_tag;
    }
    
    
    public Integer getMaxFlows() {
        return max_flows;
    }
    
    public void setMaxFlows(Integer max_flows) {
        this.max_flows = max_flows;
    }
    
}
