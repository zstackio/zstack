//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VnSubnetsType extends ApiPropertyBase {
    List<IpamSubnetType> ipam_subnets;
    RouteTableType host_routes;
    public VnSubnetsType() {
    }
    public VnSubnetsType(List<IpamSubnetType> ipam_subnets, RouteTableType host_routes) {
        this.ipam_subnets = ipam_subnets;
        this.host_routes = host_routes;
    }
    public VnSubnetsType(List<IpamSubnetType> ipam_subnets) {
        this(ipam_subnets, null);    }
    
    public RouteTableType getHostRoutes() {
        return host_routes;
    }
    
    public void setHostRoutes(RouteTableType host_routes) {
        this.host_routes = host_routes;
    }
    
    
    public List<IpamSubnetType> getIpamSubnets() {
        return ipam_subnets;
    }
    
    
    public void addIpamSubnets(IpamSubnetType obj) {
        if (ipam_subnets == null) {
            ipam_subnets = new ArrayList<IpamSubnetType>();
        }
        ipam_subnets.add(obj);
    }
    public void clearIpamSubnets() {
        ipam_subnets = null;
    }
    
}
