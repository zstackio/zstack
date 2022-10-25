//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class IpamType extends ApiPropertyBase {
    String ipam_method;
    String ipam_dns_method;
    IpamDnsAddressType ipam_dns_server;
    DhcpOptionsListType dhcp_option_list;
    SubnetType cidr_block;
    RouteTableType host_routes;
    public IpamType() {
    }
    public IpamType(String ipam_method, String ipam_dns_method, IpamDnsAddressType ipam_dns_server, DhcpOptionsListType dhcp_option_list, SubnetType cidr_block, RouteTableType host_routes) {
        this.ipam_method = ipam_method;
        this.ipam_dns_method = ipam_dns_method;
        this.ipam_dns_server = ipam_dns_server;
        this.dhcp_option_list = dhcp_option_list;
        this.cidr_block = cidr_block;
        this.host_routes = host_routes;
    }
    public IpamType(String ipam_method) {
        this(ipam_method, null, null, null, null, null);    }
    public IpamType(String ipam_method, String ipam_dns_method) {
        this(ipam_method, ipam_dns_method, null, null, null, null);    }
    public IpamType(String ipam_method, String ipam_dns_method, IpamDnsAddressType ipam_dns_server) {
        this(ipam_method, ipam_dns_method, ipam_dns_server, null, null, null);    }
    public IpamType(String ipam_method, String ipam_dns_method, IpamDnsAddressType ipam_dns_server, DhcpOptionsListType dhcp_option_list) {
        this(ipam_method, ipam_dns_method, ipam_dns_server, dhcp_option_list, null, null);    }
    public IpamType(String ipam_method, String ipam_dns_method, IpamDnsAddressType ipam_dns_server, DhcpOptionsListType dhcp_option_list, SubnetType cidr_block) {
        this(ipam_method, ipam_dns_method, ipam_dns_server, dhcp_option_list, cidr_block, null);    }
    
    public String getIpamMethod() {
        return ipam_method;
    }
    
    public void setIpamMethod(String ipam_method) {
        this.ipam_method = ipam_method;
    }
    
    
    public String getIpamDnsMethod() {
        return ipam_dns_method;
    }
    
    public void setIpamDnsMethod(String ipam_dns_method) {
        this.ipam_dns_method = ipam_dns_method;
    }
    
    
    public IpamDnsAddressType getIpamDnsServer() {
        return ipam_dns_server;
    }
    
    public void setIpamDnsServer(IpamDnsAddressType ipam_dns_server) {
        this.ipam_dns_server = ipam_dns_server;
    }
    
    
    public DhcpOptionsListType getDhcpOptionList() {
        return dhcp_option_list;
    }
    
    public void setDhcpOptionList(DhcpOptionsListType dhcp_option_list) {
        this.dhcp_option_list = dhcp_option_list;
    }
    
    
    public SubnetType getCidrBlock() {
        return cidr_block;
    }
    
    public void setCidrBlock(SubnetType cidr_block) {
        this.cidr_block = cidr_block;
    }
    
    
    public RouteTableType getHostRoutes() {
        return host_routes;
    }
    
    public void setHostRoutes(RouteTableType host_routes) {
        this.host_routes = host_routes;
    }
    
}
