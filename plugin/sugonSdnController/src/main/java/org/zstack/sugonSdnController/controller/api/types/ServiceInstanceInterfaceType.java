//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ServiceInstanceInterfaceType extends ApiPropertyBase {
    String virtual_network;
    String ip_address;
    RouteTableType static_routes;
    AllowedAddressPairs allowed_address_pairs;
    public ServiceInstanceInterfaceType() {
    }
    public ServiceInstanceInterfaceType(String virtual_network, String ip_address, RouteTableType static_routes, AllowedAddressPairs allowed_address_pairs) {
        this.virtual_network = virtual_network;
        this.ip_address = ip_address;
        this.static_routes = static_routes;
        this.allowed_address_pairs = allowed_address_pairs;
    }
    public ServiceInstanceInterfaceType(String virtual_network) {
        this(virtual_network, null, null, null);    }
    public ServiceInstanceInterfaceType(String virtual_network, String ip_address) {
        this(virtual_network, ip_address, null, null);    }
    public ServiceInstanceInterfaceType(String virtual_network, String ip_address, RouteTableType static_routes) {
        this(virtual_network, ip_address, static_routes, null);    }
    
    public String getVirtualNetwork() {
        return virtual_network;
    }
    
    public void setVirtualNetwork(String virtual_network) {
        this.virtual_network = virtual_network;
    }
    
    
    public String getIpAddress() {
        return ip_address;
    }
    
    public void setIpAddress(String ip_address) {
        this.ip_address = ip_address;
    }
    
    
    public RouteTableType getStaticRoutes() {
        return static_routes;
    }
    
    public void setStaticRoutes(RouteTableType static_routes) {
        this.static_routes = static_routes;
    }
    
    
    public AllowedAddressPairs getAllowedAddressPairs() {
        return allowed_address_pairs;
    }
    
    public void setAllowedAddressPairs(AllowedAddressPairs allowed_address_pairs) {
        this.allowed_address_pairs = allowed_address_pairs;
    }
    
}
