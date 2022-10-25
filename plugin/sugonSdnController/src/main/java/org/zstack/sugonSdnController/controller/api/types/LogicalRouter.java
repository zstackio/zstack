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

public class LogicalRouter extends ApiObjectBase {
    private RouteTargetList configured_route_target_list;
    private String vxlan_network_identifier;
    private IpAddressesType logical_router_dhcp_relay_server;
    private Boolean logical_router_gateway_external;
    private String logical_router_type;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_refs;
    private List<ObjectReference<ApiPropertyBase>> route_target_refs;
    private List<ObjectReference<ApiPropertyBase>> route_table_refs;
    private List<ObjectReference<LogicalRouterVirtualNetworkType>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> service_instance_refs;
    private List<ObjectReference<ApiPropertyBase>> physical_router_refs;
    private List<ObjectReference<ApiPropertyBase>> fabric_refs;
    private List<ObjectReference<ApiPropertyBase>> bgpvpn_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_tuple_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> data_center_interconnect_back_refs;

    @Override
    public String getObjectType() {
        return "logical-router";
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
    
    public RouteTargetList getConfiguredRouteTargetList() {
        return configured_route_target_list;
    }
    
    public void setConfiguredRouteTargetList(RouteTargetList configured_route_target_list) {
        this.configured_route_target_list = configured_route_target_list;
    }
    
    
    public String getVxlanNetworkIdentifier() {
        return vxlan_network_identifier;
    }
    
    public void setVxlanNetworkIdentifier(String vxlan_network_identifier) {
        this.vxlan_network_identifier = vxlan_network_identifier;
    }
    
    
    public IpAddressesType getDhcpRelayServer() {
        return logical_router_dhcp_relay_server;
    }
    
    public void setDhcpRelayServer(IpAddressesType logical_router_dhcp_relay_server) {
        this.logical_router_dhcp_relay_server = logical_router_dhcp_relay_server;
    }
    
    
    public Boolean getGatewayExternal() {
        return logical_router_gateway_external;
    }
    
    public void setGatewayExternal(Boolean logical_router_gateway_external) {
        this.logical_router_gateway_external = logical_router_gateway_external;
    }
    
    
    public String getType() {
        return logical_router_type;
    }
    
    public void setType(String logical_router_type) {
        this.logical_router_type = logical_router_type;
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

    public List<ObjectReference<ApiPropertyBase>> getRouteTarget() {
        return route_target_refs;
    }

    public void setRouteTarget(RouteTarget obj) {
        route_target_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        route_target_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addRouteTarget(RouteTarget obj) {
        if (route_target_refs == null) {
            route_target_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        route_target_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeRouteTarget(RouteTarget obj) {
        if (route_target_refs != null) {
            route_target_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearRouteTarget() {
        if (route_target_refs != null) {
            route_target_refs.clear();
            return;
        }
        route_target_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getRouteTable() {
        return route_table_refs;
    }

    public void setRouteTable(RouteTable obj) {
        route_table_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        route_table_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addRouteTable(RouteTable obj) {
        if (route_table_refs == null) {
            route_table_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        route_table_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeRouteTable(RouteTable obj) {
        if (route_table_refs != null) {
            route_table_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearRouteTable() {
        if (route_table_refs != null) {
            route_table_refs.clear();
            return;
        }
        route_table_refs = null;
    }

    public List<ObjectReference<LogicalRouterVirtualNetworkType>> getVirtualNetwork() {
        return virtual_network_refs;
    }

    public void setVirtualNetwork(VirtualNetwork obj, LogicalRouterVirtualNetworkType data) {
        virtual_network_refs = new ArrayList<ObjectReference<LogicalRouterVirtualNetworkType>>();
        virtual_network_refs.add(new ObjectReference<LogicalRouterVirtualNetworkType>(obj.getQualifiedName(), data));
    }

    public void addVirtualNetwork(VirtualNetwork obj, LogicalRouterVirtualNetworkType data) {
        if (virtual_network_refs == null) {
            virtual_network_refs = new ArrayList<ObjectReference<LogicalRouterVirtualNetworkType>>();
        }
        virtual_network_refs.add(new ObjectReference<LogicalRouterVirtualNetworkType>(obj.getQualifiedName(), data));
    }

    public void removeVirtualNetwork(VirtualNetwork obj, LogicalRouterVirtualNetworkType data) {
        if (virtual_network_refs != null) {
            virtual_network_refs.remove(new ObjectReference<LogicalRouterVirtualNetworkType>(obj.getQualifiedName(), data));
        }
    }

    public void clearVirtualNetwork() {
        if (virtual_network_refs != null) {
            virtual_network_refs.clear();
            return;
        }
        virtual_network_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getFabric() {
        return fabric_refs;
    }

    public void setFabric(Fabric obj) {
        fabric_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        fabric_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addFabric(Fabric obj) {
        if (fabric_refs == null) {
            fabric_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        fabric_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeFabric(Fabric obj) {
        if (fabric_refs != null) {
            fabric_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearFabric() {
        if (fabric_refs != null) {
            fabric_refs.clear();
            return;
        }
        fabric_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getBgpvpn() {
        return bgpvpn_refs;
    }

    public void setBgpvpn(Bgpvpn obj) {
        bgpvpn_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        bgpvpn_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addBgpvpn(Bgpvpn obj) {
        if (bgpvpn_refs == null) {
            bgpvpn_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        bgpvpn_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeBgpvpn(Bgpvpn obj) {
        if (bgpvpn_refs != null) {
            bgpvpn_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearBgpvpn() {
        if (bgpvpn_refs != null) {
            bgpvpn_refs.clear();
            return;
        }
        bgpvpn_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getPortTupleBackRefs() {
        return port_tuple_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDataCenterInterconnectBackRefs() {
        return data_center_interconnect_back_refs;
    }
}