//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class QuotaType extends ApiPropertyBase {
    Integer defaults;
    Integer floating_ip;
    Integer instance_ip;
    Integer virtual_machine_interface;
    Integer virtual_network;
    Integer virtual_router;
    Integer virtual_DNS;
    Integer virtual_DNS_record;
    Integer bgp_router;
    Integer network_ipam;
    Integer access_control_list;
    Integer network_policy;
    Integer floating_ip_pool;
    Integer service_template;
    Integer service_instance;
    Integer logical_router;
    Integer security_group;
    Integer security_group_rule;
    Integer subnet;
    Integer global_vrouter_config;
    Integer loadbalancer;
    Integer loadbalancer_listener;
    Integer loadbalancer_pool;
    Integer loadbalancer_member;
    Integer loadbalancer_healthmonitor;
    Integer virtual_ip;
    Integer security_logging_object;
    Integer route_table;
    Integer firewall_group;
    Integer firewall_policy;
    Integer firewall_rule;
    Integer host_based_service;
    public QuotaType() {
    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip, Integer security_logging_object, Integer route_table, Integer firewall_group, Integer firewall_policy, Integer firewall_rule, Integer host_based_service) {
        this.defaults = defaults;
        this.floating_ip = floating_ip;
        this.instance_ip = instance_ip;
        this.virtual_machine_interface = virtual_machine_interface;
        this.virtual_network = virtual_network;
        this.virtual_router = virtual_router;
        this.virtual_DNS = virtual_DNS;
        this.virtual_DNS_record = virtual_DNS_record;
        this.bgp_router = bgp_router;
        this.network_ipam = network_ipam;
        this.access_control_list = access_control_list;
        this.network_policy = network_policy;
        this.floating_ip_pool = floating_ip_pool;
        this.service_template = service_template;
        this.service_instance = service_instance;
        this.logical_router = logical_router;
        this.security_group = security_group;
        this.security_group_rule = security_group_rule;
        this.subnet = subnet;
        this.global_vrouter_config = global_vrouter_config;
        this.loadbalancer = loadbalancer;
        this.loadbalancer_listener = loadbalancer_listener;
        this.loadbalancer_pool = loadbalancer_pool;
        this.loadbalancer_member = loadbalancer_member;
        this.loadbalancer_healthmonitor = loadbalancer_healthmonitor;
        this.virtual_ip = virtual_ip;
        this.security_logging_object = security_logging_object;
        this.route_table = route_table;
        this.firewall_group = firewall_group;
        this.firewall_policy = firewall_policy;
        this.firewall_rule = firewall_rule;
        this.host_based_service = host_based_service;
    }
    public QuotaType(Integer defaults) {
        this(defaults, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip) {
        this(defaults, floating_ip, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip) {
        this(defaults, floating_ip, instance_ip, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, null, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, null, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, null, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, null, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, null, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, null, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, null, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, virtual_ip, null, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip, Integer security_logging_object) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, virtual_ip, security_logging_object, null, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip, Integer security_logging_object, Integer route_table) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, virtual_ip, security_logging_object, route_table, null, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip, Integer security_logging_object, Integer route_table, Integer firewall_group) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, virtual_ip, security_logging_object, route_table, firewall_group, null, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip, Integer security_logging_object, Integer route_table, Integer firewall_group, Integer firewall_policy) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, virtual_ip, security_logging_object, route_table, firewall_group, firewall_policy, null, 0);    }
    public QuotaType(Integer defaults, Integer floating_ip, Integer instance_ip, Integer virtual_machine_interface, Integer virtual_network, Integer virtual_router, Integer virtual_DNS, Integer virtual_DNS_record, Integer bgp_router, Integer network_ipam, Integer access_control_list, Integer network_policy, Integer floating_ip_pool, Integer service_template, Integer service_instance, Integer logical_router, Integer security_group, Integer security_group_rule, Integer subnet, Integer global_vrouter_config, Integer loadbalancer, Integer loadbalancer_listener, Integer loadbalancer_pool, Integer loadbalancer_member, Integer loadbalancer_healthmonitor, Integer virtual_ip, Integer security_logging_object, Integer route_table, Integer firewall_group, Integer firewall_policy, Integer firewall_rule) {
        this(defaults, floating_ip, instance_ip, virtual_machine_interface, virtual_network, virtual_router, virtual_DNS, virtual_DNS_record, bgp_router, network_ipam, access_control_list, network_policy, floating_ip_pool, service_template, service_instance, logical_router, security_group, security_group_rule, subnet, global_vrouter_config, loadbalancer, loadbalancer_listener, loadbalancer_pool, loadbalancer_member, loadbalancer_healthmonitor, virtual_ip, security_logging_object, route_table, firewall_group, firewall_policy, firewall_rule, 0);    }
    
    public Integer getDefaults() {
        return defaults;
    }
    
    public void setDefaults(Integer defaults) {
        this.defaults = defaults;
    }
    
    
    public Integer getFloatingIp() {
        return floating_ip;
    }
    
    public void setFloatingIp(Integer floating_ip) {
        this.floating_ip = floating_ip;
    }
    
    
    public Integer getInstanceIp() {
        return instance_ip;
    }
    
    public void setInstanceIp(Integer instance_ip) {
        this.instance_ip = instance_ip;
    }
    
    
    public Integer getVirtualMachineInterface() {
        return virtual_machine_interface;
    }
    
    public void setVirtualMachineInterface(Integer virtual_machine_interface) {
        this.virtual_machine_interface = virtual_machine_interface;
    }
    
    
    public Integer getVirtualNetwork() {
        return virtual_network;
    }
    
    public void setVirtualNetwork(Integer virtual_network) {
        this.virtual_network = virtual_network;
    }
    
    
    public Integer getVirtualRouter() {
        return virtual_router;
    }
    
    public void setVirtualRouter(Integer virtual_router) {
        this.virtual_router = virtual_router;
    }
    
    
    public Integer getVirtualDns() {
        return virtual_DNS;
    }
    
    public void setVirtualDns(Integer virtual_DNS) {
        this.virtual_DNS = virtual_DNS;
    }
    
    
    public Integer getVirtualDnsRecord() {
        return virtual_DNS_record;
    }
    
    public void setVirtualDnsRecord(Integer virtual_DNS_record) {
        this.virtual_DNS_record = virtual_DNS_record;
    }
    
    
    public Integer getBgpRouter() {
        return bgp_router;
    }
    
    public void setBgpRouter(Integer bgp_router) {
        this.bgp_router = bgp_router;
    }
    
    
    public Integer getNetworkIpam() {
        return network_ipam;
    }
    
    public void setNetworkIpam(Integer network_ipam) {
        this.network_ipam = network_ipam;
    }
    
    
    public Integer getAccessControlList() {
        return access_control_list;
    }
    
    public void setAccessControlList(Integer access_control_list) {
        this.access_control_list = access_control_list;
    }
    
    
    public Integer getNetworkPolicy() {
        return network_policy;
    }
    
    public void setNetworkPolicy(Integer network_policy) {
        this.network_policy = network_policy;
    }
    
    
    public Integer getFloatingIpPool() {
        return floating_ip_pool;
    }
    
    public void setFloatingIpPool(Integer floating_ip_pool) {
        this.floating_ip_pool = floating_ip_pool;
    }
    
    
    public Integer getServiceTemplate() {
        return service_template;
    }
    
    public void setServiceTemplate(Integer service_template) {
        this.service_template = service_template;
    }
    
    
    public Integer getServiceInstance() {
        return service_instance;
    }
    
    public void setServiceInstance(Integer service_instance) {
        this.service_instance = service_instance;
    }
    
    
    public Integer getLogicalRouter() {
        return logical_router;
    }
    
    public void setLogicalRouter(Integer logical_router) {
        this.logical_router = logical_router;
    }
    
    
    public Integer getSecurityGroup() {
        return security_group;
    }
    
    public void setSecurityGroup(Integer security_group) {
        this.security_group = security_group;
    }
    
    
    public Integer getSecurityGroupRule() {
        return security_group_rule;
    }
    
    public void setSecurityGroupRule(Integer security_group_rule) {
        this.security_group_rule = security_group_rule;
    }
    
    
    public Integer getSubnet() {
        return subnet;
    }
    
    public void setSubnet(Integer subnet) {
        this.subnet = subnet;
    }
    
    
    public Integer getGlobalVrouterConfig() {
        return global_vrouter_config;
    }
    
    public void setGlobalVrouterConfig(Integer global_vrouter_config) {
        this.global_vrouter_config = global_vrouter_config;
    }
    
    
    public Integer getLoadbalancer() {
        return loadbalancer;
    }
    
    public void setLoadbalancer(Integer loadbalancer) {
        this.loadbalancer = loadbalancer;
    }
    
    
    public Integer getLoadbalancerListener() {
        return loadbalancer_listener;
    }
    
    public void setLoadbalancerListener(Integer loadbalancer_listener) {
        this.loadbalancer_listener = loadbalancer_listener;
    }
    
    
    public Integer getLoadbalancerPool() {
        return loadbalancer_pool;
    }
    
    public void setLoadbalancerPool(Integer loadbalancer_pool) {
        this.loadbalancer_pool = loadbalancer_pool;
    }
    
    
    public Integer getLoadbalancerMember() {
        return loadbalancer_member;
    }
    
    public void setLoadbalancerMember(Integer loadbalancer_member) {
        this.loadbalancer_member = loadbalancer_member;
    }
    
    
    public Integer getLoadbalancerHealthmonitor() {
        return loadbalancer_healthmonitor;
    }
    
    public void setLoadbalancerHealthmonitor(Integer loadbalancer_healthmonitor) {
        this.loadbalancer_healthmonitor = loadbalancer_healthmonitor;
    }
    
    
    public Integer getVirtualIp() {
        return virtual_ip;
    }
    
    public void setVirtualIp(Integer virtual_ip) {
        this.virtual_ip = virtual_ip;
    }
    
    
    public Integer getSecurityLoggingObject() {
        return security_logging_object;
    }
    
    public void setSecurityLoggingObject(Integer security_logging_object) {
        this.security_logging_object = security_logging_object;
    }
    
    
    public Integer getRouteTable() {
        return route_table;
    }
    
    public void setRouteTable(Integer route_table) {
        this.route_table = route_table;
    }
    
    
    public Integer getFirewallGroup() {
        return firewall_group;
    }
    
    public void setFirewallGroup(Integer firewall_group) {
        this.firewall_group = firewall_group;
    }
    
    
    public Integer getFirewallPolicy() {
        return firewall_policy;
    }
    
    public void setFirewallPolicy(Integer firewall_policy) {
        this.firewall_policy = firewall_policy;
    }
    
    
    public Integer getFirewallRule() {
        return firewall_rule;
    }
    
    public void setFirewallRule(Integer firewall_rule) {
        this.firewall_rule = firewall_rule;
    }
    
    
    public Integer getHostBasedService() {
        return host_based_service;
    }
    
    public void setHostBasedService(Integer host_based_service) {
        this.host_based_service = host_based_service;
    }
    
}
