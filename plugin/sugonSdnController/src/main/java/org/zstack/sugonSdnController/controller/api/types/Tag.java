//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class Tag extends ApiObjectBase {
    private String tag_type_name;
    private String tag_value;
    private Boolean tag_predefined;
    private String tag_id;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> tag_type_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_endpoint_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> instance_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_appliance_set_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> route_target_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_listener_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> floating_ip_pool_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> config_root_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_template_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> hardware_inventory_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> firewall_policy_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> route_table_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> provider_attachment_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> overlay_role_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> multicast_policy_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> network_device_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_DNS_record_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> control_node_zone_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> dsa_rule_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> discovery_service_assignment_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> logical_interface_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> flow_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> route_aggregate_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> logical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> domain_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_instance_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> node_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> bridge_domain_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> alias_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> webui_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> bgp_as_a_service_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> subnet_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> global_system_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> sub_cluster_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> forwarding_class_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> address_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> application_policy_set_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> intent_map_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_tuple_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> analytics_alarm_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> netconf_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> qos_queue_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_role_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> card_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> security_logging_object_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> qos_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> analytics_snmp_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> cli_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_object_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> peering_policy_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> global_vrouter_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> floating_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> link_aggregation_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> policy_management_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> e2_service_provider_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> fabric_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> job_template_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> routing_policy_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> role_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> tag_type_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_pool_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> device_chassis_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> global_qos_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> analytics_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_DNS_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> config_database_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> config_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> device_functional_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> firewall_rule_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> bgpvpn_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> role_definition_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_connection_module_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> security_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> database_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_healthmonitor_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> devicemgr_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> project_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> fabric_namespace_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> network_ipam_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> config_properties_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> network_policy_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> sflow_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> hardware_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> tag_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> feature_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> telemetry_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> bgp_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_network_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_port_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_appliance_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> namespace_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> feature_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> storm_control_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> device_image_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_interface_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> access_control_list_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> snmp_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> grpc_profile_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> customer_attachment_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> host_based_service_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_machine_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> interface_route_table_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_member_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_health_check_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> alarm_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> api_access_list_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> routing_instance_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> alias_ip_pool_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> data_center_interconnect_back_refs;

    @Override
    public String getObjectType() {
        return "tag";
    }

    @Override
    public List<String> getDefaultParent() {
        return null;
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }

    public void setParent(ConfigRoot parent) {
        super.setParent(parent);
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public String getTypeName() {
        return tag_type_name;
    }
    
    public void setTypeName(String tag_type_name) {
        this.tag_type_name = tag_type_name;
    }
    
    
    public String getValue() {
        return tag_value;
    }
    
    public void setValue(String tag_value) {
        this.tag_value = tag_value;
    }
    
    
    public Boolean getPredefined() {
        return tag_predefined;
    }
    
    public void setPredefined(Boolean tag_predefined) {
        this.tag_predefined = tag_predefined;
    }
    
    
    public String getId() {
        return tag_id;
    }
    
    public void setId(String tag_id) {
        this.tag_id = tag_id;
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
    

    public List<ObjectReference<ApiPropertyBase>> getTagType() {
        return tag_type_refs;
    }

    public void setTagType(TagType obj) {
        tag_type_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        tag_type_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addTagType(TagType obj) {
        if (tag_type_refs == null) {
            tag_type_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        tag_type_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeTagType(TagType obj) {
        if (tag_type_refs != null) {
            tag_type_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearTagType() {
        if (tag_type_refs != null) {
            tag_type_refs.clear();
            return;
        }
        tag_type_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getServiceEndpointBackRefs() {
        return service_endpoint_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getInstanceIpBackRefs() {
        return instance_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceApplianceSetBackRefs() {
        return service_appliance_set_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRouteTargetBackRefs() {
        return route_target_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerListenerBackRefs() {
        return loadbalancer_listener_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFloatingIpPoolBackRefs() {
        return floating_ip_pool_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigRootBackRefs() {
        return config_root_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceTemplateBackRefs() {
        return service_template_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getHardwareInventoryBackRefs() {
        return hardware_inventory_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFirewallPolicyBackRefs() {
        return firewall_policy_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRouteTableBackRefs() {
        return route_table_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getProviderAttachmentBackRefs() {
        return provider_attachment_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getOverlayRoleBackRefs() {
        return overlay_role_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getMulticastPolicyBackRefs() {
        return multicast_policy_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkDeviceConfigBackRefs() {
        return network_device_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualDnsRecordBackRefs() {
        return virtual_DNS_record_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getControlNodeZoneBackRefs() {
        return control_node_zone_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDsaRuleBackRefs() {
        return dsa_rule_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDiscoveryServiceAssignmentBackRefs() {
        return discovery_service_assignment_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalInterfaceBackRefs() {
        return logical_interface_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFlowNodeBackRefs() {
        return flow_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortGroupBackRefs() {
        return port_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRouteAggregateBackRefs() {
        return route_aggregate_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouterBackRefs() {
        return logical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDomainBackRefs() {
        return domain_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceInstanceBackRefs() {
        return service_instance_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodeProfileBackRefs() {
        return node_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getBridgeDomainBackRefs() {
        return bridge_domain_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAliasIpBackRefs() {
        return alias_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getWebuiNodeBackRefs() {
        return webui_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortBackRefs() {
        return port_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getBgpAsAServiceBackRefs() {
        return bgp_as_a_service_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getSubnetBackRefs() {
        return subnet_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getGlobalSystemConfigBackRefs() {
        return global_system_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getSubClusterBackRefs() {
        return sub_cluster_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getForwardingClassBackRefs() {
        return forwarding_class_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceGroupBackRefs() {
        return service_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAddressGroupBackRefs() {
        return address_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getApplicationPolicySetBackRefs() {
        return application_policy_set_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualIpBackRefs() {
        return virtual_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getIntentMapBackRefs() {
        return intent_map_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortTupleBackRefs() {
        return port_tuple_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAnalyticsAlarmNodeBackRefs() {
        return analytics_alarm_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetconfProfileBackRefs() {
        return netconf_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getQosQueueBackRefs() {
        return qos_queue_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRoleBackRefs() {
        return physical_role_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getCardBackRefs() {
        return card_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getSecurityLoggingObjectBackRefs() {
        return security_logging_object_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getQosConfigBackRefs() {
        return qos_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAnalyticsSnmpNodeBackRefs() {
        return analytics_snmp_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterfaceBackRefs() {
        return virtual_machine_interface_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getCliConfigBackRefs() {
        return cli_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceObjectBackRefs() {
        return service_object_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerBackRefs() {
        return loadbalancer_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPeeringPolicyBackRefs() {
        return peering_policy_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getGlobalVrouterConfigBackRefs() {
        return global_vrouter_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFloatingIpBackRefs() {
        return floating_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLinkAggregationGroupBackRefs() {
        return link_aggregation_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualRouterBackRefs() {
        return virtual_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortProfileBackRefs() {
        return port_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPolicyManagementBackRefs() {
        return policy_management_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getE2ServiceProviderBackRefs() {
        return e2_service_provider_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFabricBackRefs() {
        return fabric_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getJobTemplateBackRefs() {
        return job_template_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoutingPolicyBackRefs() {
        return routing_policy_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoleConfigBackRefs() {
        return role_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getTagTypeBackRefs() {
        return tag_type_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerPoolBackRefs() {
        return loadbalancer_pool_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceChassisBackRefs() {
        return device_chassis_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getGlobalQosConfigBackRefs() {
        return global_qos_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAnalyticsNodeBackRefs() {
        return analytics_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualDnsBackRefs() {
        return virtual_DNS_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigDatabaseNodeBackRefs() {
        return config_database_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigNodeBackRefs() {
        return config_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceFunctionalGroupBackRefs() {
        return device_functional_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFirewallRuleBackRefs() {
        return firewall_rule_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getBgpvpnBackRefs() {
        return bgpvpn_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoleDefinitionBackRefs() {
        return role_definition_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceConnectionModuleBackRefs() {
        return service_connection_module_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getSecurityGroupBackRefs() {
        return security_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDatabaseNodeBackRefs() {
        return database_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerHealthmonitorBackRefs() {
        return loadbalancer_healthmonitor_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDevicemgrNodeBackRefs() {
        return devicemgr_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getProjectBackRefs() {
        return project_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFabricNamespaceBackRefs() {
        return fabric_namespace_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkIpamBackRefs() {
        return network_ipam_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigPropertiesBackRefs() {
        return config_properties_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkPolicyBackRefs() {
        return network_policy_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getSflowProfileBackRefs() {
        return sflow_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getHardwareBackRefs() {
        return hardware_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getTagBackRefs() {
        return tag_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFeatureConfigBackRefs() {
        return feature_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getTelemetryProfileBackRefs() {
        return telemetry_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getBgpRouterBackRefs() {
        return bgp_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetworkBackRefs() {
        return virtual_network_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualPortGroupBackRefs() {
        return virtual_port_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceApplianceBackRefs() {
        return service_appliance_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNamespaceBackRefs() {
        return namespace_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFeatureBackRefs() {
        return feature_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getStormControlProfileBackRefs() {
        return storm_control_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceImageBackRefs() {
        return device_image_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalInterfaceBackRefs() {
        return physical_interface_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAccessControlListBackRefs() {
        return access_control_list_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getSnmpProfileBackRefs() {
        return snmp_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodeBackRefs() {
        return node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getGrpcProfileBackRefs() {
        return grpc_profile_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getCustomerAttachmentBackRefs() {
        return customer_attachment_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getHostBasedServiceBackRefs() {
        return host_based_service_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineBackRefs() {
        return virtual_machine_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getInterfaceRouteTableBackRefs() {
        return interface_route_table_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerMemberBackRefs() {
        return loadbalancer_member_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceHealthCheckBackRefs() {
        return service_health_check_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAlarmBackRefs() {
        return alarm_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getApiAccessListBackRefs() {
        return api_access_list_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoutingInstanceBackRefs() {
        return routing_instance_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAliasIpPoolBackRefs() {
        return alias_ip_pool_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDataCenterInterconnectBackRefs() {
        return data_center_interconnect_back_refs;
    }
}