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

public class InstanceIp extends ApiObjectBase {
    private String instance_ip_address;
    private String instance_ip_family;
    private String instance_ip_mode;
    private SubnetType secondary_ip_tracking_ip;
    private String subnet_uuid;
    private String instance_ip_subscriber_tag;
    private Boolean instance_ip_secondary;
    private Boolean instance_ip_local_ip;
    private Boolean service_instance_ip;
    private Boolean service_health_check_ip;
    private SubnetType instance_ip_subnet;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> network_ipam_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> physical_router_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_router_refs;
    private List<ObjectReference<ApiPropertyBase>> logical_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> flow_node_refs;
    private List<ObjectReference<ApiPropertyBase>> floating_ips;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ServiceInterfaceTag>> service_instance_back_refs;

    @Override
    public String getObjectType() {
        return "instance-ip";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList();
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }
    
    public String getAddress() {
        return instance_ip_address;
    }
    
    public void setAddress(String instance_ip_address) {
        this.instance_ip_address = instance_ip_address;
    }
    
    
    public String getFamily() {
        return instance_ip_family;
    }
    
    public void setFamily(String instance_ip_family) {
        this.instance_ip_family = instance_ip_family;
    }
    
    
    public String getMode() {
        return instance_ip_mode;
    }
    
    public void setMode(String instance_ip_mode) {
        this.instance_ip_mode = instance_ip_mode;
    }
    
    
    public SubnetType getSecondaryIpTrackingIp() {
        return secondary_ip_tracking_ip;
    }
    
    public void setSecondaryIpTrackingIp(SubnetType secondary_ip_tracking_ip) {
        this.secondary_ip_tracking_ip = secondary_ip_tracking_ip;
    }
    
    
    public String getSubnetUuid() {
        return subnet_uuid;
    }
    
    public void setSubnetUuid(String subnet_uuid) {
        this.subnet_uuid = subnet_uuid;
    }
    
    
    public String getSubscriberTag() {
        return instance_ip_subscriber_tag;
    }
    
    public void setSubscriberTag(String instance_ip_subscriber_tag) {
        this.instance_ip_subscriber_tag = instance_ip_subscriber_tag;
    }
    
    
    public Boolean getSecondary() {
        return instance_ip_secondary;
    }
    
    public void setSecondary(Boolean instance_ip_secondary) {
        this.instance_ip_secondary = instance_ip_secondary;
    }
    
    
    public Boolean getLocalIp() {
        return instance_ip_local_ip;
    }
    
    public void setLocalIp(Boolean instance_ip_local_ip) {
        this.instance_ip_local_ip = instance_ip_local_ip;
    }
    
    
    public Boolean getServiceInstanceIp() {
        return service_instance_ip;
    }
    
    public void setServiceInstanceIp(Boolean service_instance_ip) {
        this.service_instance_ip = service_instance_ip;
    }
    
    
    public Boolean getServiceHealthCheckIp() {
        return service_health_check_ip;
    }
    
    public void setServiceHealthCheckIp(Boolean service_health_check_ip) {
        this.service_health_check_ip = service_health_check_ip;
    }
    
    
    public SubnetType getSubnet() {
        return instance_ip_subnet;
    }
    
    public void setSubnet(SubnetType instance_ip_subnet) {
        this.instance_ip_subnet = instance_ip_subnet;
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
    

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetwork() {
        return virtual_network_refs;
    }

    public void setVirtualNetwork(VirtualNetwork obj) {
        virtual_network_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_network_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualNetwork(VirtualNetwork obj) {
        if (virtual_network_refs == null) {
            virtual_network_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_network_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualNetwork(VirtualNetwork obj) {
        if (virtual_network_refs != null) {
            virtual_network_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualNetwork() {
        if (virtual_network_refs != null) {
            virtual_network_refs.clear();
            return;
        }
        virtual_network_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkIpam() {
        return network_ipam_refs;
    }

    public void setNetworkIpam(NetworkIpam obj) {
        network_ipam_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        network_ipam_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addNetworkIpam(NetworkIpam obj) {
        if (network_ipam_refs == null) {
            network_ipam_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        network_ipam_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeNetworkIpam(NetworkIpam obj) {
        if (network_ipam_refs != null) {
            network_ipam_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearNetworkIpam() {
        if (network_ipam_refs != null) {
            network_ipam_refs.clear();
            return;
        }
        network_ipam_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getVirtualRouter() {
        return virtual_router_refs;
    }

    public void setVirtualRouter(VirtualRouter obj) {
        virtual_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualRouter(VirtualRouter obj) {
        if (virtual_router_refs == null) {
            virtual_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualRouter(VirtualRouter obj) {
        if (virtual_router_refs != null) {
            virtual_router_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualRouter() {
        if (virtual_router_refs != null) {
            virtual_router_refs.clear();
            return;
        }
        virtual_router_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalInterface() {
        return logical_interface_refs;
    }

    public void setLogicalInterface(LogicalInterface obj) {
        logical_interface_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        logical_interface_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addLogicalInterface(LogicalInterface obj) {
        if (logical_interface_refs == null) {
            logical_interface_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        logical_interface_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeLogicalInterface(LogicalInterface obj) {
        if (logical_interface_refs != null) {
            logical_interface_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearLogicalInterface() {
        if (logical_interface_refs != null) {
            logical_interface_refs.clear();
            return;
        }
        logical_interface_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getFlowNode() {
        return flow_node_refs;
    }

    public void setFlowNode(FlowNode obj) {
        flow_node_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        flow_node_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addFlowNode(FlowNode obj) {
        if (flow_node_refs == null) {
            flow_node_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        flow_node_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeFlowNode(FlowNode obj) {
        if (flow_node_refs != null) {
            flow_node_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearFlowNode() {
        if (flow_node_refs != null) {
            flow_node_refs.clear();
            return;
        }
        flow_node_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getFloatingIps() {
        return floating_ips;
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

    public List<ObjectReference<ServiceInterfaceTag>> getServiceInstanceBackRefs() {
        return service_instance_back_refs;
    }
}