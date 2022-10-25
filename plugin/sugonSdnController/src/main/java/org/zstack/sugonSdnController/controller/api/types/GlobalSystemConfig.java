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

public class GlobalSystemConfig extends ApiObjectBase {
    private Integer autonomous_system;
    private Boolean enable_4byte_as;
    private String config_version;
    private GracefulRestartParametersType graceful_restart_parameters;
    private FastConvergenceParametersType fast_convergence_parameters;
    private PluginProperties plugin_tuning;
    private SubnetListType data_center_interconnect_loopback_namespace;
    private AsnRangeType data_center_interconnect_asn_namespace;
    private Boolean ibgp_auto_mesh;
    private Boolean bgp_always_compare_med;
    private Integer rd_cluster_seed;
    private SubnetListType ip_fabric_subnets;
    private DeviceFamilyListType supported_device_families;
    private VendorHardwaresType supported_vendor_hardwares;
    private BGPaaServiceParametersType bgpaas_parameters;
    private MACLimitControlType mac_limit_control;
    private MACMoveLimitControlType mac_move_control;
    private Integer mac_aging_time;
    private Boolean igmp_enable;
    private Boolean alarm_enable;
    private UserDefinedLogStatList user_defined_log_statistics;
    private Boolean enable_security_policy_draft;
    private KeyValuePairs supported_fabric_annotations;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> bgp_router_refs;
    private List<ObjectReference<ApiPropertyBase>> control_node_zones;
    private List<ObjectReference<ApiPropertyBase>> global_vrouter_configs;
    private List<ObjectReference<ApiPropertyBase>> global_qos_configs;
    private List<ObjectReference<ApiPropertyBase>> virtual_routers;
    private List<ObjectReference<ApiPropertyBase>> config_nodes;
    private List<ObjectReference<ApiPropertyBase>> analytics_nodes;
    private List<ObjectReference<ApiPropertyBase>> flow_nodes;
    private List<ObjectReference<ApiPropertyBase>> devicemgr_nodes;
    private List<ObjectReference<ApiPropertyBase>> database_nodes;
    private List<ObjectReference<ApiPropertyBase>> webui_nodes;
    private List<ObjectReference<ApiPropertyBase>> config_database_nodes;
    private List<ObjectReference<ApiPropertyBase>> analytics_alarm_nodes;
    private List<ObjectReference<ApiPropertyBase>> analytics_snmp_nodes;
    private List<ObjectReference<ApiPropertyBase>> service_appliance_sets;
    private List<ObjectReference<ApiPropertyBase>> api_access_lists;
    private List<ObjectReference<ApiPropertyBase>> alarms;
    private List<ObjectReference<ApiPropertyBase>> config_propertiess;
    private List<ObjectReference<ApiPropertyBase>> job_templates;
    private List<ObjectReference<ApiPropertyBase>> data_center_interconnects;
    private List<ObjectReference<ApiPropertyBase>> intent_maps;
    private List<ObjectReference<ApiPropertyBase>> fabrics;
    private List<ObjectReference<ApiPropertyBase>> node_profiles;
    private List<ObjectReference<ApiPropertyBase>> physical_routers;
    private List<ObjectReference<ApiPropertyBase>> device_images;
    private List<ObjectReference<ApiPropertyBase>> nodes;
    private List<ObjectReference<ApiPropertyBase>> features;
    private List<ObjectReference<ApiPropertyBase>> physical_roles;
    private List<ObjectReference<ApiPropertyBase>> overlay_roles;
    private List<ObjectReference<ApiPropertyBase>> role_definitions;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> qos_config_back_refs;

    @Override
    public String getObjectType() {
        return "global-system-config";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList();
    }

    @Override
    public String getDefaultParentType() {
        return "config-root";
    }

    public void setParent(ConfigRoot parent) {
        super.setParent(parent);
    }
    
    public Integer getAutonomousSystem() {
        return autonomous_system;
    }
    
    public void setAutonomousSystem(Integer autonomous_system) {
        this.autonomous_system = autonomous_system;
    }
    
    
    public Boolean getEnable4byteAs() {
        return enable_4byte_as;
    }
    
    public void setEnable4byteAs(Boolean enable_4byte_as) {
        this.enable_4byte_as = enable_4byte_as;
    }
    
    
    public String getConfigVersion() {
        return config_version;
    }
    
    public void setConfigVersion(String config_version) {
        this.config_version = config_version;
    }
    
    
    public GracefulRestartParametersType getGracefulRestartParameters() {
        return graceful_restart_parameters;
    }
    
    public void setGracefulRestartParameters(GracefulRestartParametersType graceful_restart_parameters) {
        this.graceful_restart_parameters = graceful_restart_parameters;
    }
    
    
    public FastConvergenceParametersType getFastConvergenceParameters() {
        return fast_convergence_parameters;
    }
    
    public void setFastConvergenceParameters(FastConvergenceParametersType fast_convergence_parameters) {
        this.fast_convergence_parameters = fast_convergence_parameters;
    }
    
    
    public PluginProperties getPluginTuning() {
        return plugin_tuning;
    }
    
    public void setPluginTuning(PluginProperties plugin_tuning) {
        this.plugin_tuning = plugin_tuning;
    }
    
    
    public SubnetListType getDataCenterInterconnectLoopbackNamespace() {
        return data_center_interconnect_loopback_namespace;
    }
    
    public void setDataCenterInterconnectLoopbackNamespace(SubnetListType data_center_interconnect_loopback_namespace) {
        this.data_center_interconnect_loopback_namespace = data_center_interconnect_loopback_namespace;
    }
    
    
    public AsnRangeType getDataCenterInterconnectAsnNamespace() {
        return data_center_interconnect_asn_namespace;
    }
    
    public void setDataCenterInterconnectAsnNamespace(AsnRangeType data_center_interconnect_asn_namespace) {
        this.data_center_interconnect_asn_namespace = data_center_interconnect_asn_namespace;
    }
    
    
    public Boolean getIbgpAutoMesh() {
        return ibgp_auto_mesh;
    }
    
    public void setIbgpAutoMesh(Boolean ibgp_auto_mesh) {
        this.ibgp_auto_mesh = ibgp_auto_mesh;
    }
    
    
    public Boolean getBgpAlwaysCompareMed() {
        return bgp_always_compare_med;
    }
    
    public void setBgpAlwaysCompareMed(Boolean bgp_always_compare_med) {
        this.bgp_always_compare_med = bgp_always_compare_med;
    }
    
    
    public Integer getRdClusterSeed() {
        return rd_cluster_seed;
    }
    
    public void setRdClusterSeed(Integer rd_cluster_seed) {
        this.rd_cluster_seed = rd_cluster_seed;
    }
    
    
    public SubnetListType getIpFabricSubnets() {
        return ip_fabric_subnets;
    }
    
    public void setIpFabricSubnets(SubnetListType ip_fabric_subnets) {
        this.ip_fabric_subnets = ip_fabric_subnets;
    }
    
    
    public DeviceFamilyListType getSupportedDeviceFamilies() {
        return supported_device_families;
    }
    
    public void setSupportedDeviceFamilies(DeviceFamilyListType supported_device_families) {
        this.supported_device_families = supported_device_families;
    }
    
    
    public VendorHardwaresType getSupportedVendorHardwares() {
        return supported_vendor_hardwares;
    }
    
    public void setSupportedVendorHardwares(VendorHardwaresType supported_vendor_hardwares) {
        this.supported_vendor_hardwares = supported_vendor_hardwares;
    }
    
    
    public BGPaaServiceParametersType getBgpaasParameters() {
        return bgpaas_parameters;
    }
    
    public void setBgpaasParameters(BGPaaServiceParametersType bgpaas_parameters) {
        this.bgpaas_parameters = bgpaas_parameters;
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
    
    
    public Boolean getIgmpEnable() {
        return igmp_enable;
    }
    
    public void setIgmpEnable(Boolean igmp_enable) {
        this.igmp_enable = igmp_enable;
    }
    
    
    public Boolean getAlarmEnable() {
        return alarm_enable;
    }
    
    public void setAlarmEnable(Boolean alarm_enable) {
        this.alarm_enable = alarm_enable;
    }
    
    
    public UserDefinedLogStatList getUserDefinedLogStatistics() {
        return user_defined_log_statistics;
    }
    
    public void setUserDefinedLogStatistics(UserDefinedLogStatList user_defined_log_statistics) {
        this.user_defined_log_statistics = user_defined_log_statistics;
    }
    
    
    public Boolean getEnableSecurityPolicyDraft() {
        return enable_security_policy_draft;
    }
    
    public void setEnableSecurityPolicyDraft(Boolean enable_security_policy_draft) {
        this.enable_security_policy_draft = enable_security_policy_draft;
    }
    
    
    public KeyValuePairs getSupportedFabricAnnotations() {
        return supported_fabric_annotations;
    }
    
    public void setSupportedFabricAnnotations(KeyValuePairs supported_fabric_annotations) {
        this.supported_fabric_annotations = supported_fabric_annotations;
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
    

    public List<ObjectReference<ApiPropertyBase>> getBgpRouter() {
        return bgp_router_refs;
    }

    public void setBgpRouter(BgpRouter obj) {
        bgp_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        bgp_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addBgpRouter(BgpRouter obj) {
        if (bgp_router_refs == null) {
            bgp_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        bgp_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeBgpRouter(BgpRouter obj) {
        if (bgp_router_refs != null) {
            bgp_router_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearBgpRouter() {
        if (bgp_router_refs != null) {
            bgp_router_refs.clear();
            return;
        }
        bgp_router_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getControlNodeZones() {
        return control_node_zones;
    }

    public List<ObjectReference<ApiPropertyBase>> getGlobalVrouterConfigs() {
        return global_vrouter_configs;
    }

    public List<ObjectReference<ApiPropertyBase>> getGlobalQosConfigs() {
        return global_qos_configs;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualRouters() {
        return virtual_routers;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigNodes() {
        return config_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getAnalyticsNodes() {
        return analytics_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getFlowNodes() {
        return flow_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getDevicemgrNodes() {
        return devicemgr_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getDatabaseNodes() {
        return database_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getWebuiNodes() {
        return webui_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigDatabaseNodes() {
        return config_database_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getAnalyticsAlarmNodes() {
        return analytics_alarm_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getAnalyticsSnmpNodes() {
        return analytics_snmp_nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceApplianceSets() {
        return service_appliance_sets;
    }

    public List<ObjectReference<ApiPropertyBase>> getApiAccessLists() {
        return api_access_lists;
    }

    public List<ObjectReference<ApiPropertyBase>> getAlarms() {
        return alarms;
    }

    public List<ObjectReference<ApiPropertyBase>> getConfigPropertiess() {
        return config_propertiess;
    }

    public List<ObjectReference<ApiPropertyBase>> getJobTemplates() {
        return job_templates;
    }

    public List<ObjectReference<ApiPropertyBase>> getDataCenterInterconnects() {
        return data_center_interconnects;
    }

    public List<ObjectReference<ApiPropertyBase>> getIntentMaps() {
        return intent_maps;
    }

    public List<ObjectReference<ApiPropertyBase>> getFabrics() {
        return fabrics;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodeProfiles() {
        return node_profiles;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouters() {
        return physical_routers;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceImages() {
        return device_images;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodes() {
        return nodes;
    }

    public List<ObjectReference<ApiPropertyBase>> getFeatures() {
        return features;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRoles() {
        return physical_roles;
    }

    public List<ObjectReference<ApiPropertyBase>> getOverlayRoles() {
        return overlay_roles;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoleDefinitions() {
        return role_definitions;
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

    public List<ObjectReference<ApiPropertyBase>> getQosConfigBackRefs() {
        return qos_config_back_refs;
    }
}