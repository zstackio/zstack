//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class IpamSubnetType extends ApiPropertyBase {
    SubnetType subnet;
    String default_gateway;
    String dns_server_address;
    String subnet_uuid;
    Boolean enable_dhcp;
    List<String> dns_nameservers;
    List<AllocationPoolType> allocation_pools;
    Boolean addr_from_start;
    DhcpOptionsListType dhcp_option_list;
    RouteTableType host_routes;
    String subnet_name;
    Integer alloc_unit;
    volatile java.util.Date created;
    volatile java.util.Date last_modified;
    String subscriber_tag;
    Integer vlan_tag;
    List<String> dhcp_relay_server;
    public IpamSubnetType() {
    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name, Integer alloc_unit, java.util.Date created, java.util.Date last_modified, String subscriber_tag, Integer vlan_tag, List<String> dhcp_relay_server) {
        this.subnet = subnet;
        this.default_gateway = default_gateway;
        this.dns_server_address = dns_server_address;
        this.subnet_uuid = subnet_uuid;
        this.enable_dhcp = enable_dhcp;
        this.dns_nameservers = dns_nameservers;
        this.allocation_pools = allocation_pools;
        this.addr_from_start = addr_from_start;
        this.dhcp_option_list = dhcp_option_list;
        this.host_routes = host_routes;
        this.subnet_name = subnet_name;
        this.alloc_unit = alloc_unit;
        this.created = created;
        this.last_modified = last_modified;
        this.subscriber_tag = subscriber_tag;
        this.vlan_tag = vlan_tag;
        this.dhcp_relay_server = dhcp_relay_server;
    }
    public IpamSubnetType(SubnetType subnet) {
        this(subnet, null, null, null, true, null, null, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway) {
        this(subnet, default_gateway, null, null, true, null, null, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address) {
        this(subnet, default_gateway, dns_server_address, null, true, null, null, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, true, null, null, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, null, null, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, null, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, null, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, null, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, null, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, null, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, subnet_name, 1, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name, Integer alloc_unit) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, subnet_name, alloc_unit, null, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name, Integer alloc_unit, java.util.Date created) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, subnet_name, alloc_unit, created, null, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name, Integer alloc_unit, java.util.Date created, java.util.Date last_modified) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, subnet_name, alloc_unit, created, last_modified, null, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name, Integer alloc_unit, java.util.Date created, java.util.Date last_modified, String subscriber_tag) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, subnet_name, alloc_unit, created, last_modified, subscriber_tag, null, null);    }
    public IpamSubnetType(SubnetType subnet, String default_gateway, String dns_server_address, String subnet_uuid, Boolean enable_dhcp, List<String> dns_nameservers, List<AllocationPoolType> allocation_pools, Boolean addr_from_start, DhcpOptionsListType dhcp_option_list, RouteTableType host_routes, String subnet_name, Integer alloc_unit, java.util.Date created, java.util.Date last_modified, String subscriber_tag, Integer vlan_tag) {
        this(subnet, default_gateway, dns_server_address, subnet_uuid, enable_dhcp, dns_nameservers, allocation_pools, addr_from_start, dhcp_option_list, host_routes, subnet_name, alloc_unit, created, last_modified, subscriber_tag, vlan_tag, null);    }
    
    public SubnetType getSubnet() {
        return subnet;
    }
    
    public void setSubnet(SubnetType subnet) {
        this.subnet = subnet;
    }
    
    
    public String getDefaultGateway() {
        return default_gateway;
    }
    
    public void setDefaultGateway(String default_gateway) {
        this.default_gateway = default_gateway;
    }
    
    
    public String getDnsServerAddress() {
        return dns_server_address;
    }
    
    public void setDnsServerAddress(String dns_server_address) {
        this.dns_server_address = dns_server_address;
    }
    
    
    public String getSubnetUuid() {
        return subnet_uuid;
    }
    
    public void setSubnetUuid(String subnet_uuid) {
        this.subnet_uuid = subnet_uuid;
    }
    
    
    public Boolean getEnableDhcp() {
        return enable_dhcp;
    }
    
    public void setEnableDhcp(Boolean enable_dhcp) {
        this.enable_dhcp = enable_dhcp;
    }
    
    
    public Boolean getAddrFromStart() {
        return addr_from_start;
    }
    
    public void setAddrFromStart(Boolean addr_from_start) {
        this.addr_from_start = addr_from_start;
    }
    
    
    public DhcpOptionsListType getDhcpOptionList() {
        return dhcp_option_list;
    }
    
    public void setDhcpOptionList(DhcpOptionsListType dhcp_option_list) {
        this.dhcp_option_list = dhcp_option_list;
    }
    
    
    public RouteTableType getHostRoutes() {
        return host_routes;
    }
    
    public void setHostRoutes(RouteTableType host_routes) {
        this.host_routes = host_routes;
    }
    
    
    public String getSubnetName() {
        return subnet_name;
    }
    
    public void setSubnetName(String subnet_name) {
        this.subnet_name = subnet_name;
    }
    
    
    public Integer getAllocUnit() {
        return alloc_unit;
    }
    
    public void setAllocUnit(Integer alloc_unit) {
        this.alloc_unit = alloc_unit;
    }
    
    
    public java.util.Date getCreated() {
        return created;
    }
    
    public void setCreated(java.util.Date created) {
        this.created = created;
    }
    
    
    public java.util.Date getLastModified() {
        return last_modified;
    }
    
    public void setLastModified(java.util.Date last_modified) {
        this.last_modified = last_modified;
    }
    
    
    public String getSubscriberTag() {
        return subscriber_tag;
    }
    
    public void setSubscriberTag(String subscriber_tag) {
        this.subscriber_tag = subscriber_tag;
    }
    
    
    public Integer getVlanTag() {
        return vlan_tag;
    }
    
    public void setVlanTag(Integer vlan_tag) {
        this.vlan_tag = vlan_tag;
    }
    
    
    public List<String> getDnsNameservers() {
        return dns_nameservers;
    }
    
    
    public void addDnsNameservers(String obj) {
        if (dns_nameservers == null) {
            dns_nameservers = new ArrayList<String>();
        }
        dns_nameservers.add(obj);
    }
    public void clearDnsNameservers() {
        dns_nameservers = null;
    }
    
    
    public List<AllocationPoolType> getAllocationPools() {
        return allocation_pools;
    }
    
    
    public void addAllocationPools(AllocationPoolType obj) {
        if (allocation_pools == null) {
            allocation_pools = new ArrayList<AllocationPoolType>();
        }
        allocation_pools.add(obj);
    }
    public void clearAllocationPools() {
        allocation_pools = null;
    }
    
    
    public void addAllocationPools(String start, String end, Boolean vrouter_specific_pool) {
        if (allocation_pools == null) {
            allocation_pools = new ArrayList<AllocationPoolType>();
        }
        allocation_pools.add(new AllocationPoolType(start, end, vrouter_specific_pool));
    }
    
    
    public List<String> getDhcpRelayServer() {
        return dhcp_relay_server;
    }
    
    
    public void addDhcpRelayServer(String obj) {
        if (dhcp_relay_server == null) {
            dhcp_relay_server = new ArrayList<String>();
        }
        dhcp_relay_server.add(obj);
    }
    public void clearDhcpRelayServer() {
        dhcp_relay_server = null;
    }
    
}
