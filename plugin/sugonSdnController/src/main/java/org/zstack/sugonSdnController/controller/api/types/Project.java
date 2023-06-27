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

public class Project extends ApiObjectBase {
    private QuotaType quota;
    private Boolean vxlan_routing;
    private Boolean alarm_enable;
    private Boolean enable_security_policy_draft;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> security_logging_objects;
    private List<ObjectReference<SubnetType>> namespace_refs;
    private List<ObjectReference<ApiPropertyBase>> security_groups;
    private List<ObjectReference<ApiPropertyBase>> virtual_networks;
    private List<ObjectReference<ApiPropertyBase>> qos_configs;
    private List<ObjectReference<ApiPropertyBase>> network_ipams;
    private List<ObjectReference<ApiPropertyBase>> network_policys;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interfaces;
    private List<ObjectReference<ApiPropertyBase>> floating_ip_pool_refs;
    private List<ObjectReference<ApiPropertyBase>> alias_ip_pool_refs;
    private List<ObjectReference<ApiPropertyBase>> bgp_as_a_services;
    private List<ObjectReference<ApiPropertyBase>> routing_policys;
    private List<ObjectReference<ApiPropertyBase>> route_aggregates;
    private List<ObjectReference<ApiPropertyBase>> service_instances;
    private List<ObjectReference<ApiPropertyBase>> service_health_checks;
    private List<ObjectReference<ApiPropertyBase>> route_tables;
    private List<ObjectReference<ApiPropertyBase>> interface_route_tables;
    private List<ObjectReference<ApiPropertyBase>> logical_routers;
    private List<ObjectReference<ApiPropertyBase>> api_access_lists;
    private List<ObjectReference<ApiPropertyBase>> multicast_policys;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_pools;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_healthmonitors;
    private List<ObjectReference<ApiPropertyBase>> virtual_ips;
    private List<ObjectReference<ApiPropertyBase>> loadbalancer_listeners;
    private List<ObjectReference<ApiPropertyBase>> loadbalancers;
    private List<ObjectReference<ApiPropertyBase>> bgpvpns;
    private List<ObjectReference<ApiPropertyBase>> alarms;
    private List<ObjectReference<ApiPropertyBase>> policy_managements;
    private List<ObjectReference<ApiPropertyBase>> service_groups;
    private List<ObjectReference<ApiPropertyBase>> address_groups;
    private List<ObjectReference<ApiPropertyBase>> firewall_rules;
    private List<ObjectReference<ApiPropertyBase>> firewall_policys;
    private List<ObjectReference<ApiPropertyBase>> application_policy_sets;
    private List<ObjectReference<ApiPropertyBase>> application_policy_set_refs;
    private List<ObjectReference<ApiPropertyBase>> tags;
    private List<ObjectReference<ApiPropertyBase>> device_functional_groups;
    private List<ObjectReference<ApiPropertyBase>> virtual_port_groups;
    private List<ObjectReference<ApiPropertyBase>> telemetry_profiles;
    private List<ObjectReference<ApiPropertyBase>> sflow_profiles;
    private List<ObjectReference<ApiPropertyBase>> grpc_profiles;
    private List<ObjectReference<ApiPropertyBase>> snmp_profiles;
    private List<ObjectReference<ApiPropertyBase>> netconf_profiles;
    private List<ObjectReference<ApiPropertyBase>> storm_control_profiles;
    private List<ObjectReference<ApiPropertyBase>> port_profiles;
    private List<ObjectReference<ApiPropertyBase>> host_based_services;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> floating_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> alias_ip_back_refs;

    @Override
    public String getObjectType() {
        return "project";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain");
    }

    @Override
    public String getDefaultParentType() {
        return "domain";
    }

    public void setParent(Domain parent) {
        super.setParent(parent);
    }
    
    public QuotaType getQuota() {
        return quota;
    }
    
    public void setQuota(QuotaType quota) {
        this.quota = quota;
    }
    
    
    public Boolean getVxlanRouting() {
        return vxlan_routing;
    }
    
    public void setVxlanRouting(Boolean vxlan_routing) {
        this.vxlan_routing = vxlan_routing;
    }
    
    
    public Boolean getAlarmEnable() {
        return alarm_enable;
    }
    
    public void setAlarmEnable(Boolean alarm_enable) {
        this.alarm_enable = alarm_enable;
    }
    
    
    public Boolean getEnableSecurityPolicyDraft() {
        return enable_security_policy_draft;
    }
    
    public void setEnableSecurityPolicyDraft(Boolean enable_security_policy_draft) {
        this.enable_security_policy_draft = enable_security_policy_draft;
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
    

    public List<ObjectReference<ApiPropertyBase>> getSecurityLoggingObjects() {
        return security_logging_objects;
    }

    public List<ObjectReference<SubnetType>> getNamespace() {
        return namespace_refs;
    }

    public void setNamespace(Namespace obj, SubnetType data) {
        namespace_refs = new ArrayList<ObjectReference<SubnetType>>();
        namespace_refs.add(new ObjectReference<SubnetType>(obj.getQualifiedName(), data));
    }

    public void addNamespace(Namespace obj, SubnetType data) {
        if (namespace_refs == null) {
            namespace_refs = new ArrayList<ObjectReference<SubnetType>>();
        }
        namespace_refs.add(new ObjectReference<SubnetType>(obj.getQualifiedName(), data));
    }

    public void removeNamespace(Namespace obj, SubnetType data) {
        if (namespace_refs != null) {
            namespace_refs.remove(new ObjectReference<SubnetType>(obj.getQualifiedName(), data));
        }
    }

    public void clearNamespace() {
        if (namespace_refs != null) {
            namespace_refs.clear();
            return;
        }
        namespace_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getSecurityGroups() {
        return security_groups;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetworks() {
        return virtual_networks;
    }

    public List<ObjectReference<ApiPropertyBase>> getQosConfigs() {
        return qos_configs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkIpams() {
        return network_ipams;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkPolicys() {
        return network_policys;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterfaces() {
        return virtual_machine_interfaces;
    }

    public List<ObjectReference<ApiPropertyBase>> getFloatingIpPool() {
        return floating_ip_pool_refs;
    }

    public void setFloatingIpPool(FloatingIpPool obj) {
        floating_ip_pool_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        floating_ip_pool_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addFloatingIpPool(FloatingIpPool obj) {
        if (floating_ip_pool_refs == null) {
            floating_ip_pool_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        floating_ip_pool_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeFloatingIpPool(FloatingIpPool obj) {
        if (floating_ip_pool_refs != null) {
            floating_ip_pool_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearFloatingIpPool() {
        if (floating_ip_pool_refs != null) {
            floating_ip_pool_refs.clear();
            return;
        }
        floating_ip_pool_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getAliasIpPool() {
        return alias_ip_pool_refs;
    }

    public void setAliasIpPool(AliasIpPool obj) {
        alias_ip_pool_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        alias_ip_pool_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addAliasIpPool(AliasIpPool obj) {
        if (alias_ip_pool_refs == null) {
            alias_ip_pool_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        alias_ip_pool_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeAliasIpPool(AliasIpPool obj) {
        if (alias_ip_pool_refs != null) {
            alias_ip_pool_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearAliasIpPool() {
        if (alias_ip_pool_refs != null) {
            alias_ip_pool_refs.clear();
            return;
        }
        alias_ip_pool_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getBgpAsAServices() {
        return bgp_as_a_services;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoutingPolicys() {
        return routing_policys;
    }

    public List<ObjectReference<ApiPropertyBase>> getRouteAggregates() {
        return route_aggregates;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceInstances() {
        return service_instances;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceHealthChecks() {
        return service_health_checks;
    }

    public List<ObjectReference<ApiPropertyBase>> getRouteTables() {
        return route_tables;
    }

    public List<ObjectReference<ApiPropertyBase>> getInterfaceRouteTables() {
        return interface_route_tables;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouters() {
        return logical_routers;
    }

    public List<ObjectReference<ApiPropertyBase>> getApiAccessLists() {
        return api_access_lists;
    }

    public List<ObjectReference<ApiPropertyBase>> getMulticastPolicys() {
        return multicast_policys;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerPools() {
        return loadbalancer_pools;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerHealthmonitors() {
        return loadbalancer_healthmonitors;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualIps() {
        return virtual_ips;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerListeners() {
        return loadbalancer_listeners;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancers() {
        return loadbalancers;
    }

    public List<ObjectReference<ApiPropertyBase>> getBgpvpns() {
        return bgpvpns;
    }

    public List<ObjectReference<ApiPropertyBase>> getAlarms() {
        return alarms;
    }

    public List<ObjectReference<ApiPropertyBase>> getPolicyManagements() {
        return policy_managements;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceGroups() {
        return service_groups;
    }

    public List<ObjectReference<ApiPropertyBase>> getAddressGroups() {
        return address_groups;
    }

    public List<ObjectReference<ApiPropertyBase>> getFirewallRules() {
        return firewall_rules;
    }

    public List<ObjectReference<ApiPropertyBase>> getFirewallPolicys() {
        return firewall_policys;
    }

    public List<ObjectReference<ApiPropertyBase>> getApplicationPolicySets() {
        return application_policy_sets;
    }

    public List<ObjectReference<ApiPropertyBase>> getApplicationPolicySet() {
        return application_policy_set_refs;
    }

    public void setApplicationPolicySet(ApplicationPolicySet obj) {
        application_policy_set_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        application_policy_set_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addApplicationPolicySet(ApplicationPolicySet obj) {
        if (application_policy_set_refs == null) {
            application_policy_set_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        application_policy_set_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeApplicationPolicySet(ApplicationPolicySet obj) {
        if (application_policy_set_refs != null) {
            application_policy_set_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearApplicationPolicySet() {
        if (application_policy_set_refs != null) {
            application_policy_set_refs.clear();
            return;
        }
        application_policy_set_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getTags() {
        return tags;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceFunctionalGroups() {
        return device_functional_groups;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualPortGroups() {
        return virtual_port_groups;
    }

    public List<ObjectReference<ApiPropertyBase>> getTelemetryProfiles() {
        return telemetry_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getSflowProfiles() {
        return sflow_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getGrpcProfiles() {
        return grpc_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getSnmpProfiles() {
        return snmp_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetconfProfiles() {
        return netconf_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getStormControlProfiles() {
        return storm_control_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortProfiles() {
        return port_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getHostBasedServices() {
        return host_based_services;
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

    public List<ObjectReference<ApiPropertyBase>> getFloatingIpBackRefs() {
        return floating_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getAliasIpBackRefs() {
        return alias_ip_back_refs;
    }
}