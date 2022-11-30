//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RoutedProperties extends ApiPropertyBase {
    String physical_router_uuid;
    String logical_router_uuid;
    String routed_interface_ip_address;
    String loopback_ip_address;
    String routing_protocol;
    BgpParameters bgp_params;
    OspfParameters ospf_params;
    PimParameters pim_params;
    StaticRouteParameters static_route_params;
    BfdParameters bfd_params;
    RoutingPolicyParameters routing_policy_params;
    public RoutedProperties() {
    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol, BgpParameters bgp_params, OspfParameters ospf_params, PimParameters pim_params, StaticRouteParameters static_route_params, BfdParameters bfd_params, RoutingPolicyParameters routing_policy_params) {
        this.physical_router_uuid = physical_router_uuid;
        this.logical_router_uuid = logical_router_uuid;
        this.routed_interface_ip_address = routed_interface_ip_address;
        this.loopback_ip_address = loopback_ip_address;
        this.routing_protocol = routing_protocol;
        this.bgp_params = bgp_params;
        this.ospf_params = ospf_params;
        this.pim_params = pim_params;
        this.static_route_params = static_route_params;
        this.bfd_params = bfd_params;
        this.routing_policy_params = routing_policy_params;
    }
    public RoutedProperties(String physical_router_uuid) {
        this(physical_router_uuid, null, null, null, null, null, null, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid) {
        this(physical_router_uuid, logical_router_uuid, null, null, null, null, null, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, null, null, null, null, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, null, null, null, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, routing_protocol, null, null, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol, BgpParameters bgp_params) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, routing_protocol, bgp_params, null, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol, BgpParameters bgp_params, OspfParameters ospf_params) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, routing_protocol, bgp_params, ospf_params, null, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol, BgpParameters bgp_params, OspfParameters ospf_params, PimParameters pim_params) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, routing_protocol, bgp_params, ospf_params, pim_params, null, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol, BgpParameters bgp_params, OspfParameters ospf_params, PimParameters pim_params, StaticRouteParameters static_route_params) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, routing_protocol, bgp_params, ospf_params, pim_params, static_route_params, null, null);    }
    public RoutedProperties(String physical_router_uuid, String logical_router_uuid, String routed_interface_ip_address, String loopback_ip_address, String routing_protocol, BgpParameters bgp_params, OspfParameters ospf_params, PimParameters pim_params, StaticRouteParameters static_route_params, BfdParameters bfd_params) {
        this(physical_router_uuid, logical_router_uuid, routed_interface_ip_address, loopback_ip_address, routing_protocol, bgp_params, ospf_params, pim_params, static_route_params, bfd_params, null);    }
    
    public String getPhysicalRouterUuid() {
        return physical_router_uuid;
    }
    
    public void setPhysicalRouterUuid(String physical_router_uuid) {
        this.physical_router_uuid = physical_router_uuid;
    }
    
    
    public String getLogicalRouterUuid() {
        return logical_router_uuid;
    }
    
    public void setLogicalRouterUuid(String logical_router_uuid) {
        this.logical_router_uuid = logical_router_uuid;
    }
    
    
    public String getRoutedInterfaceIpAddress() {
        return routed_interface_ip_address;
    }
    
    public void setRoutedInterfaceIpAddress(String routed_interface_ip_address) {
        this.routed_interface_ip_address = routed_interface_ip_address;
    }
    
    
    public String getLoopbackIpAddress() {
        return loopback_ip_address;
    }
    
    public void setLoopbackIpAddress(String loopback_ip_address) {
        this.loopback_ip_address = loopback_ip_address;
    }
    
    
    public String getRoutingProtocol() {
        return routing_protocol;
    }
    
    public void setRoutingProtocol(String routing_protocol) {
        this.routing_protocol = routing_protocol;
    }
    
    
    public BgpParameters getBgpParams() {
        return bgp_params;
    }
    
    public void setBgpParams(BgpParameters bgp_params) {
        this.bgp_params = bgp_params;
    }
    
    
    public OspfParameters getOspfParams() {
        return ospf_params;
    }
    
    public void setOspfParams(OspfParameters ospf_params) {
        this.ospf_params = ospf_params;
    }
    
    
    public PimParameters getPimParams() {
        return pim_params;
    }
    
    public void setPimParams(PimParameters pim_params) {
        this.pim_params = pim_params;
    }
    
    
    public StaticRouteParameters getStaticRouteParams() {
        return static_route_params;
    }
    
    public void setStaticRouteParams(StaticRouteParameters static_route_params) {
        this.static_route_params = static_route_params;
    }
    
    
    public BfdParameters getBfdParams() {
        return bfd_params;
    }
    
    public void setBfdParams(BfdParameters bfd_params) {
        this.bfd_params = bfd_params;
    }
    
    
    public RoutingPolicyParameters getRoutingPolicyParams() {
        return routing_policy_params;
    }
    
    public void setRoutingPolicyParams(RoutingPolicyParameters routing_policy_params) {
        this.routing_policy_params = routing_policy_params;
    }
    
}
