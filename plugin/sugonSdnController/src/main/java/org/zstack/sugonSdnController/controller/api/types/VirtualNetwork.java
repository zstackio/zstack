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

public class VirtualNetwork extends ApiObjectBase {
    private EcmpHashingIncludeFields ecmp_hashing_include_fields;
    private String virtual_network_category;
    private VirtualNetworkType virtual_network_properties;
    private VirtualNetworkRoutedPropertiesType virtual_network_routed_properties;
    private ProviderDetails provider_properties;
    private Integer virtual_network_network_id;
    private Boolean is_provider_network;
    private Boolean port_security_enabled;
    private Boolean fabric_snat;
    private RouteTargetList route_target_list;
    private RouteTargetList import_route_target_list;
    private RouteTargetList export_route_target_list;
    private Boolean router_external;
    private Boolean is_shared;
    private Boolean external_ipam;
    private Boolean flood_unknown_unicast;
    private Boolean multi_policy_service_chains_enabled;
    private String address_allocation_mode;
    private IPSegmentType ip_segment;
    private FatFlowProtocols virtual_network_fat_flow_protocols;
    private Boolean mac_learning_enabled;
    private MACLimitControlType mac_limit_control;
    private MACMoveLimitControlType mac_move_control;
    private Integer mac_aging_time;
    private Boolean pbb_evpn_enable;
    private Boolean pbb_etree_enable;
    private Boolean layer2_control_word;
    private Boolean igmp_enable;
    private Boolean mac_ip_learning_enable;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> security_logging_object_refs;
    private List<ObjectReference<ApiPropertyBase>> qos_config_refs;
    private List<ObjectReference<VnSubnetsType>> network_ipam_refs;
    private List<ObjectReference<VirtualNetworkPolicyType>> network_policy_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> access_control_lists;
    private List<ObjectReference<ApiPropertyBase>> floating_ip_pools;
    private List<ObjectReference<ApiPropertyBase>> alias_ip_pools;
    private List<ObjectReference<ApiPropertyBase>> routing_instances;
    private List<ObjectReference<ApiPropertyBase>> service_health_check_refs;
    private List<ObjectReference<ApiPropertyBase>> route_table_refs;
    private List<ObjectReference<ApiPropertyBase>> bridge_domains;
    private List<ObjectReference<ApiPropertyBase>> multicast_policy_refs;
    private List<ObjectReference<ApiPropertyBase>> bgpvpn_refs;
    private List<ObjectReference<ApiPropertyBase>> intent_map_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_network_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_machine_interface_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> instance_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_tuple_back_refs;
    private transient List<ObjectReference<LogicalRouterVirtualNetworkType>> logical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> flow_node_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> firewall_rule_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> data_center_interconnect_back_refs;
    private transient List<ObjectReference<FabricNetworkTag>> fabric_back_refs;
    private transient List<ObjectReference<ServiceVirtualNetworkType>> host_based_service_back_refs;

    @Override
    public String getObjectType() {
        return "virtual-network";
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

    public EcmpHashingIncludeFields getEcmpHashingIncludeFields() {
        return ecmp_hashing_include_fields;
    }

    public void setEcmpHashingIncludeFields(EcmpHashingIncludeFields ecmp_hashing_include_fields) {
        this.ecmp_hashing_include_fields = ecmp_hashing_include_fields;
    }


    public String getCategory() {
        return virtual_network_category;
    }

    public void setCategory(String virtual_network_category) {
        this.virtual_network_category = virtual_network_category;
    }


    public VirtualNetworkType getProperties() {
        return virtual_network_properties;
    }

    public void setProperties(VirtualNetworkType virtual_network_properties) {
        this.virtual_network_properties = virtual_network_properties;
    }


    public VirtualNetworkRoutedPropertiesType getRoutedProperties() {
        return virtual_network_routed_properties;
    }

    public void setRoutedProperties(VirtualNetworkRoutedPropertiesType virtual_network_routed_properties) {
        this.virtual_network_routed_properties = virtual_network_routed_properties;
    }


    public ProviderDetails getProviderProperties() {
        return provider_properties;
    }

    public void setProviderProperties(ProviderDetails provider_properties) {
        this.provider_properties = provider_properties;
    }


    public Integer getNetworkId() {
        return virtual_network_network_id;
    }

    public void setNetworkId(Integer virtual_network_network_id) {
        this.virtual_network_network_id = virtual_network_network_id;
    }


    public Boolean getIsProviderNetwork() {
        return is_provider_network;
    }

    public void setIsProviderNetwork(Boolean is_provider_network) {
        this.is_provider_network = is_provider_network;
    }


    public Boolean getPortSecurityEnabled() {
        return port_security_enabled;
    }

    public void setPortSecurityEnabled(Boolean port_security_enabled) {
        this.port_security_enabled = port_security_enabled;
    }


    public Boolean getFabricSnat() {
        return fabric_snat;
    }

    public void setFabricSnat(Boolean fabric_snat) {
        this.fabric_snat = fabric_snat;
    }


    public RouteTargetList getRouteTargetList() {
        return route_target_list;
    }

    public void setRouteTargetList(RouteTargetList route_target_list) {
        this.route_target_list = route_target_list;
    }


    public RouteTargetList getImportRouteTargetList() {
        return import_route_target_list;
    }

    public void setImportRouteTargetList(RouteTargetList import_route_target_list) {
        this.import_route_target_list = import_route_target_list;
    }


    public RouteTargetList getExportRouteTargetList() {
        return export_route_target_list;
    }

    public void setExportRouteTargetList(RouteTargetList export_route_target_list) {
        this.export_route_target_list = export_route_target_list;
    }


    public Boolean getRouterExternal() {
        return router_external;
    }

    public void setRouterExternal(Boolean router_external) {
        this.router_external = router_external;
    }


    public Boolean getIsShared() {
        return is_shared;
    }

    public void setIsShared(Boolean is_shared) {
        this.is_shared = is_shared;
    }


    public Boolean getExternalIpam() {
        return external_ipam;
    }

    public void setExternalIpam(Boolean external_ipam) {
        this.external_ipam = external_ipam;
    }


    public Boolean getFloodUnknownUnicast() {
        return flood_unknown_unicast;
    }

    public void setFloodUnknownUnicast(Boolean flood_unknown_unicast) {
        this.flood_unknown_unicast = flood_unknown_unicast;
    }


    public Boolean getMultiPolicyServiceChainsEnabled() {
        return multi_policy_service_chains_enabled;
    }

    public void setMultiPolicyServiceChainsEnabled(Boolean multi_policy_service_chains_enabled) {
        this.multi_policy_service_chains_enabled = multi_policy_service_chains_enabled;
    }


    public String getAddressAllocationMode() {
        return address_allocation_mode;
    }

    public void setAddressAllocationMode(String address_allocation_mode) {
        this.address_allocation_mode = address_allocation_mode;
    }


    public IPSegmentType getIpSegment() {
        return ip_segment;
    }

    public void setIpSegment(IPSegmentType ip_segment) {
        this.ip_segment = ip_segment;
    }


    public FatFlowProtocols getFatFlowProtocols() {
        return virtual_network_fat_flow_protocols;
    }

    public void setFatFlowProtocols(FatFlowProtocols virtual_network_fat_flow_protocols) {
        this.virtual_network_fat_flow_protocols = virtual_network_fat_flow_protocols;
    }


    public Boolean getMacLearningEnabled() {
        return mac_learning_enabled;
    }

    public void setMacLearningEnabled(Boolean mac_learning_enabled) {
        this.mac_learning_enabled = mac_learning_enabled;
    }


    public MACLimitControlType getMacLimitControl() {
        return mac_limit_control;
    }

    public void setMacLimitControl(MACLimitControlType mac_limit_control) {
        this.mac_limit_control = mac_limit_control;
    }


    public MACMoveLimitControlType getMacMoveControl() {
        return mac_move_control;
    }

    public void setMacMoveControl(MACMoveLimitControlType mac_move_control) {
        this.mac_move_control = mac_move_control;
    }


    public Integer getMacAgingTime() {
        return mac_aging_time;
    }

    public void setMacAgingTime(Integer mac_aging_time) {
        this.mac_aging_time = mac_aging_time;
    }


    public Boolean getPbbEvpnEnable() {
        return pbb_evpn_enable;
    }

    public void setPbbEvpnEnable(Boolean pbb_evpn_enable) {
        this.pbb_evpn_enable = pbb_evpn_enable;
    }


    public Boolean getPbbEtreeEnable() {
        return pbb_etree_enable;
    }

    public void setPbbEtreeEnable(Boolean pbb_etree_enable) {
        this.pbb_etree_enable = pbb_etree_enable;
    }


    public Boolean getLayer2ControlWord() {
        return layer2_control_word;
    }

    public void setLayer2ControlWord(Boolean layer2_control_word) {
        this.layer2_control_word = layer2_control_word;
    }


    public Boolean getIgmpEnable() {
        return igmp_enable;
    }

    public void setIgmpEnable(Boolean igmp_enable) {
        this.igmp_enable = igmp_enable;
    }


    public Boolean getMacIpLearningEnable() {
        return mac_ip_learning_enable;
    }

    public void setMacIpLearningEnable(Boolean mac_ip_learning_enable) {
        this.mac_ip_learning_enable = mac_ip_learning_enable;
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


    public List<ObjectReference<ApiPropertyBase>> getSecurityLoggingObject() {
        return security_logging_object_refs;
    }

    public void setSecurityLoggingObject(SecurityLoggingObject obj) {
        security_logging_object_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        security_logging_object_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void addSecurityLoggingObject(SecurityLoggingObject obj) {
        if (security_logging_object_refs == null) {
            security_logging_object_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        security_logging_object_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void removeSecurityLoggingObject(SecurityLoggingObject obj) {
        if (security_logging_object_refs != null) {
            security_logging_object_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearSecurityLoggingObject() {
        if (security_logging_object_refs != null) {
            security_logging_object_refs.clear();
            return;
        }
        security_logging_object_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getQosConfig() {
        return qos_config_refs;
    }

    public void setQosConfig(QosConfig obj) {
        qos_config_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        qos_config_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void addQosConfig(QosConfig obj) {
        if (qos_config_refs == null) {
            qos_config_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        qos_config_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void removeQosConfig(QosConfig obj) {
        if (qos_config_refs != null) {
            qos_config_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearQosConfig() {
        if (qos_config_refs != null) {
            qos_config_refs.clear();
            return;
        }
        qos_config_refs = null;
    }

    public List<ObjectReference<VnSubnetsType>> getNetworkIpam() {
        return network_ipam_refs;
    }

    public void setNetworkIpam(NetworkIpam obj, VnSubnetsType data) {
        network_ipam_refs = new ArrayList<ObjectReference<VnSubnetsType>>();
        network_ipam_refs.add(new ObjectReference<VnSubnetsType>(obj.getQualifiedName(), data));
    }

    public void addNetworkIpam(NetworkIpam obj, VnSubnetsType data) {
        if (network_ipam_refs == null) {
            network_ipam_refs = new ArrayList<ObjectReference<VnSubnetsType>>();
        }
        network_ipam_refs.add(new ObjectReference<VnSubnetsType>(obj.getQualifiedName(), data));
    }

    public void removeNetworkIpam(NetworkIpam obj, VnSubnetsType data) {
        if (network_ipam_refs != null) {
            network_ipam_refs.remove(new ObjectReference<VnSubnetsType>(obj.getQualifiedName(), data));
        }
    }

    public void clearNetworkIpam() {
        if (network_ipam_refs != null) {
            network_ipam_refs.clear();
            return;
        }
        network_ipam_refs = null;
    }


    public List<ObjectReference<VirtualNetworkPolicyType>> getNetworkPolicy() {
        return network_policy_refs;
    }

    public void setNetworkPolicy(NetworkPolicy obj, VirtualNetworkPolicyType data) {
        network_policy_refs = new ArrayList<ObjectReference<VirtualNetworkPolicyType>>();
        network_policy_refs.add(new ObjectReference<VirtualNetworkPolicyType>(obj.getQualifiedName(), data));
    }

    public void addNetworkPolicy(NetworkPolicy obj, VirtualNetworkPolicyType data) {
        if (network_policy_refs == null) {
            network_policy_refs = new ArrayList<ObjectReference<VirtualNetworkPolicyType>>();
        }
        network_policy_refs.add(new ObjectReference<VirtualNetworkPolicyType>(obj.getQualifiedName(), data));
    }

    public void removeNetworkPolicy(NetworkPolicy obj, VirtualNetworkPolicyType data) {
        if (network_policy_refs != null) {
            network_policy_refs.remove(new ObjectReference<VirtualNetworkPolicyType>(obj.getQualifiedName(), data));
        }
    }

    public void clearNetworkPolicy() {
        if (network_policy_refs != null) {
            network_policy_refs.clear();
            return;
        }
        network_policy_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getAccessControlLists() {
        return access_control_lists;
    }

    public List<ObjectReference<ApiPropertyBase>> getFloatingIpPools() {
        return floating_ip_pools;
    }

    public List<ObjectReference<ApiPropertyBase>> getAliasIpPools() {
        return alias_ip_pools;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoutingInstances() {
        return routing_instances;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceHealthCheck() {
        return service_health_check_refs;
    }

    public void setServiceHealthCheck(ServiceHealthCheck obj) {
        service_health_check_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        service_health_check_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void addServiceHealthCheck(ServiceHealthCheck obj) {
        if (service_health_check_refs == null) {
            service_health_check_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        service_health_check_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void removeServiceHealthCheck(ServiceHealthCheck obj) {
        if (service_health_check_refs != null) {
            service_health_check_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearServiceHealthCheck() {
        if (service_health_check_refs != null) {
            service_health_check_refs.clear();
            return;
        }
        service_health_check_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getBridgeDomains() {
        return bridge_domains;
    }

    public List<ObjectReference<ApiPropertyBase>> getMulticastPolicy() {
        return multicast_policy_refs;
    }

    public void setMulticastPolicy(MulticastPolicy obj) {
        multicast_policy_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        multicast_policy_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void addMulticastPolicy(MulticastPolicy obj) {
        if (multicast_policy_refs == null) {
            multicast_policy_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        multicast_policy_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void removeMulticastPolicy(MulticastPolicy obj) {
        if (multicast_policy_refs != null) {
            multicast_policy_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearMulticastPolicy() {
        if (multicast_policy_refs != null) {
            multicast_policy_refs.clear();
            return;
        }
        multicast_policy_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getIntentMap() {
        return intent_map_refs;
    }

    public void setIntentMap(IntentMap obj) {
        intent_map_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        intent_map_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void addIntentMap(IntentMap obj) {
        if (intent_map_refs == null) {
            intent_map_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        intent_map_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }

    public void removeIntentMap(IntentMap obj) {
        if (intent_map_refs != null) {
            intent_map_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearIntentMap() {
        if (intent_map_refs != null) {
            intent_map_refs.clear();
            return;
        }
        intent_map_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetworkBackRefs() {
        return virtual_network_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterfaceBackRefs() {
        return virtual_machine_interface_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getInstanceIpBackRefs() {
        return instance_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortTupleBackRefs() {
        return port_tuple_back_refs;
    }

    public List<ObjectReference<LogicalRouterVirtualNetworkType>> getLogicalRouterBackRefs() {
        return logical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFlowNodeBackRefs() {
        return flow_node_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getFirewallRuleBackRefs() {
        return firewall_rule_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDataCenterInterconnectBackRefs() {
        return data_center_interconnect_back_refs;
    }

    public List<ObjectReference<FabricNetworkTag>> getFabricBackRefs() {
        return fabric_back_refs;
    }

    public List<ObjectReference<ServiceVirtualNetworkType>> getHostBasedServiceBackRefs() {
        return host_based_service_back_refs;
    }
}