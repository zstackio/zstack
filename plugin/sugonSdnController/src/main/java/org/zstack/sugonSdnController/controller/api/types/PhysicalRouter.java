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

public class PhysicalRouter extends ApiObjectBase {
    private JunosServicePorts physical_router_junos_service_ports;
    private TelemetryStateInfo telemetry_info;
    private String physical_router_device_family;
    private String physical_router_os_version;
    private String physical_router_hostname;
    private String physical_router_management_ip;
    private String physical_router_management_mac;
    private String physical_router_dataplane_ip;
    private String physical_router_loopback_ip;
    private String physical_router_replicator_loopback_ip;
    private String physical_router_vendor_name;
    private String physical_router_product_name;
    private String physical_router_serial_number;
    private Boolean physical_router_vnc_managed;
    private Boolean physical_router_underlay_managed;
    private String physical_router_role;
    private RoutingBridgingRolesType routing_bridging_roles;
    private Boolean physical_router_snmp;
    private Boolean physical_router_lldp;
    private UserCredentials physical_router_user_credentials;
    private String physical_router_encryption_type;
    private SNMPCredentials physical_router_snmp_credentials;
    private DnsmasqLeaseParameters physical_router_dhcp_parameters;
    private String physical_router_cli_commit_state;
    private String physical_router_managed_state;
    private String physical_router_onboarding_state;
    private String physical_router_underlay_config;
    private String physical_router_supplemental_config;
    private AutonomousSystemsType physical_router_autonomous_system;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> virtual_router_refs;
    private List<ObjectReference<ApiPropertyBase>> bgp_router_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> intent_map_refs;
    private List<ObjectReference<ApiPropertyBase>> fabric_refs;
    private List<ObjectReference<ApiPropertyBase>> node_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> device_functional_group_refs;
    private List<ObjectReference<ApiPropertyBase>> device_chassis_refs;
    private List<ObjectReference<ApiPropertyBase>> device_image_refs;
    private List<ObjectReference<ApiPropertyBase>> link_aggregation_groups;
    private List<ObjectReference<ApiPropertyBase>> physical_role_refs;
    private List<ObjectReference<ApiPropertyBase>> overlay_role_refs;
    private List<ObjectReference<ApiPropertyBase>> hardware_inventorys;
    private List<ObjectReference<ApiPropertyBase>> cli_configs;
    private List<ObjectReference<ApiPropertyBase>> physical_interfaces;
    private List<ObjectReference<ApiPropertyBase>> logical_interfaces;
    private List<ObjectReference<ApiPropertyBase>> telemetry_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> instance_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> logical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_endpoint_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> network_device_config_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> e2_service_provider_back_refs;

    @Override
    public String getObjectType() {
        return "physical-router";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config");
    }

    @Override
    public String getDefaultParentType() {
        return "global-system-config";
    }

    public void setParent(GlobalSystemConfig parent) {
        super.setParent(parent);
    }
    
    public JunosServicePorts getJunosServicePorts() {
        return physical_router_junos_service_ports;
    }
    
    public void setJunosServicePorts(JunosServicePorts physical_router_junos_service_ports) {
        this.physical_router_junos_service_ports = physical_router_junos_service_ports;
    }
    
    
    public TelemetryStateInfo getTelemetryInfo() {
        return telemetry_info;
    }
    
    public void setTelemetryInfo(TelemetryStateInfo telemetry_info) {
        this.telemetry_info = telemetry_info;
    }
    
    
    public String getDeviceFamily() {
        return physical_router_device_family;
    }
    
    public void setDeviceFamily(String physical_router_device_family) {
        this.physical_router_device_family = physical_router_device_family;
    }
    
    
    public String getOsVersion() {
        return physical_router_os_version;
    }
    
    public void setOsVersion(String physical_router_os_version) {
        this.physical_router_os_version = physical_router_os_version;
    }
    
    
    public String getHostname() {
        return physical_router_hostname;
    }
    
    public void setHostname(String physical_router_hostname) {
        this.physical_router_hostname = physical_router_hostname;
    }
    
    
    public String getManagementIp() {
        return physical_router_management_ip;
    }
    
    public void setManagementIp(String physical_router_management_ip) {
        this.physical_router_management_ip = physical_router_management_ip;
    }
    
    
    public String getManagementMac() {
        return physical_router_management_mac;
    }
    
    public void setManagementMac(String physical_router_management_mac) {
        this.physical_router_management_mac = physical_router_management_mac;
    }
    
    
    public String getDataplaneIp() {
        return physical_router_dataplane_ip;
    }
    
    public void setDataplaneIp(String physical_router_dataplane_ip) {
        this.physical_router_dataplane_ip = physical_router_dataplane_ip;
    }
    
    
    public String getLoopbackIp() {
        return physical_router_loopback_ip;
    }
    
    public void setLoopbackIp(String physical_router_loopback_ip) {
        this.physical_router_loopback_ip = physical_router_loopback_ip;
    }
    
    
    public String getReplicatorLoopbackIp() {
        return physical_router_replicator_loopback_ip;
    }
    
    public void setReplicatorLoopbackIp(String physical_router_replicator_loopback_ip) {
        this.physical_router_replicator_loopback_ip = physical_router_replicator_loopback_ip;
    }
    
    
    public String getVendorName() {
        return physical_router_vendor_name;
    }
    
    public void setVendorName(String physical_router_vendor_name) {
        this.physical_router_vendor_name = physical_router_vendor_name;
    }
    
    
    public String getProductName() {
        return physical_router_product_name;
    }
    
    public void setProductName(String physical_router_product_name) {
        this.physical_router_product_name = physical_router_product_name;
    }
    
    
    public String getSerialNumber() {
        return physical_router_serial_number;
    }
    
    public void setSerialNumber(String physical_router_serial_number) {
        this.physical_router_serial_number = physical_router_serial_number;
    }
    
    
    public Boolean getVncManaged() {
        return physical_router_vnc_managed;
    }
    
    public void setVncManaged(Boolean physical_router_vnc_managed) {
        this.physical_router_vnc_managed = physical_router_vnc_managed;
    }
    
    
    public Boolean getUnderlayManaged() {
        return physical_router_underlay_managed;
    }
    
    public void setUnderlayManaged(Boolean physical_router_underlay_managed) {
        this.physical_router_underlay_managed = physical_router_underlay_managed;
    }
    
    
    public String getRole() {
        return physical_router_role;
    }
    
    public void setRole(String physical_router_role) {
        this.physical_router_role = physical_router_role;
    }
    
    
    public RoutingBridgingRolesType getRoutingBridgingRoles() {
        return routing_bridging_roles;
    }
    
    public void setRoutingBridgingRoles(RoutingBridgingRolesType routing_bridging_roles) {
        this.routing_bridging_roles = routing_bridging_roles;
    }
    
    
    public Boolean getSnmp() {
        return physical_router_snmp;
    }
    
    public void setSnmp(Boolean physical_router_snmp) {
        this.physical_router_snmp = physical_router_snmp;
    }
    
    
    public Boolean getLldp() {
        return physical_router_lldp;
    }
    
    public void setLldp(Boolean physical_router_lldp) {
        this.physical_router_lldp = physical_router_lldp;
    }
    
    
    public UserCredentials getUserCredentials() {
        return physical_router_user_credentials;
    }
    
    public void setUserCredentials(UserCredentials physical_router_user_credentials) {
        this.physical_router_user_credentials = physical_router_user_credentials;
    }
    
    
    public String getEncryptionType() {
        return physical_router_encryption_type;
    }
    
    public void setEncryptionType(String physical_router_encryption_type) {
        this.physical_router_encryption_type = physical_router_encryption_type;
    }
    
    
    public SNMPCredentials getSnmpCredentials() {
        return physical_router_snmp_credentials;
    }
    
    public void setSnmpCredentials(SNMPCredentials physical_router_snmp_credentials) {
        this.physical_router_snmp_credentials = physical_router_snmp_credentials;
    }
    
    
    public DnsmasqLeaseParameters getDhcpParameters() {
        return physical_router_dhcp_parameters;
    }
    
    public void setDhcpParameters(DnsmasqLeaseParameters physical_router_dhcp_parameters) {
        this.physical_router_dhcp_parameters = physical_router_dhcp_parameters;
    }
    
    
    public String getCliCommitState() {
        return physical_router_cli_commit_state;
    }
    
    public void setCliCommitState(String physical_router_cli_commit_state) {
        this.physical_router_cli_commit_state = physical_router_cli_commit_state;
    }
    
    
    public String getManagedState() {
        return physical_router_managed_state;
    }
    
    public void setManagedState(String physical_router_managed_state) {
        this.physical_router_managed_state = physical_router_managed_state;
    }
    
    
    public String getOnboardingState() {
        return physical_router_onboarding_state;
    }
    
    public void setOnboardingState(String physical_router_onboarding_state) {
        this.physical_router_onboarding_state = physical_router_onboarding_state;
    }
    
    
    public String getUnderlayConfig() {
        return physical_router_underlay_config;
    }
    
    public void setUnderlayConfig(String physical_router_underlay_config) {
        this.physical_router_underlay_config = physical_router_underlay_config;
    }
    
    
    public String getSupplementalConfig() {
        return physical_router_supplemental_config;
    }
    
    public void setSupplementalConfig(String physical_router_supplemental_config) {
        this.physical_router_supplemental_config = physical_router_supplemental_config;
    }
    
    
    public AutonomousSystemsType getAutonomousSystem() {
        return physical_router_autonomous_system;
    }
    
    public void setAutonomousSystem(AutonomousSystemsType physical_router_autonomous_system) {
        this.physical_router_autonomous_system = physical_router_autonomous_system;
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
    

    public List<ObjectReference<ApiPropertyBase>> getVirtualRouter() {
        return virtual_router_refs;
    }

    public void setVirtualRouter(VirtualRouter obj) {
        virtual_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualRouter(VirtualRouter obj) {
        if (virtual_router_refs == null) {
            virtual_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualRouter(VirtualRouter obj) {
        if (virtual_router_refs != null) {
            virtual_router_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualRouter() {
        if (virtual_router_refs != null) {
            virtual_router_refs.clear();
            return;
        }
        virtual_router_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getFabric() {
        return fabric_refs;
    }

    public void setFabric(Fabric obj) {
        fabric_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        fabric_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addFabric(Fabric obj) {
        if (fabric_refs == null) {
            fabric_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        fabric_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeFabric(Fabric obj) {
        if (fabric_refs != null) {
            fabric_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearFabric() {
        if (fabric_refs != null) {
            fabric_refs.clear();
            return;
        }
        fabric_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodeProfile() {
        return node_profile_refs;
    }

    public void setNodeProfile(NodeProfile obj) {
        node_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        node_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addNodeProfile(NodeProfile obj) {
        if (node_profile_refs == null) {
            node_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        node_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeNodeProfile(NodeProfile obj) {
        if (node_profile_refs != null) {
            node_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearNodeProfile() {
        if (node_profile_refs != null) {
            node_profile_refs.clear();
            return;
        }
        node_profile_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceFunctionalGroup() {
        return device_functional_group_refs;
    }

    public void setDeviceFunctionalGroup(DeviceFunctionalGroup obj) {
        device_functional_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        device_functional_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addDeviceFunctionalGroup(DeviceFunctionalGroup obj) {
        if (device_functional_group_refs == null) {
            device_functional_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        device_functional_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeDeviceFunctionalGroup(DeviceFunctionalGroup obj) {
        if (device_functional_group_refs != null) {
            device_functional_group_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearDeviceFunctionalGroup() {
        if (device_functional_group_refs != null) {
            device_functional_group_refs.clear();
            return;
        }
        device_functional_group_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceChassis() {
        return device_chassis_refs;
    }

    public void setDeviceChassis(DeviceChassis obj) {
        device_chassis_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        device_chassis_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addDeviceChassis(DeviceChassis obj) {
        if (device_chassis_refs == null) {
            device_chassis_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        device_chassis_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeDeviceChassis(DeviceChassis obj) {
        if (device_chassis_refs != null) {
            device_chassis_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearDeviceChassis() {
        if (device_chassis_refs != null) {
            device_chassis_refs.clear();
            return;
        }
        device_chassis_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getDeviceImage() {
        return device_image_refs;
    }

    public void setDeviceImage(DeviceImage obj) {
        device_image_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        device_image_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addDeviceImage(DeviceImage obj) {
        if (device_image_refs == null) {
            device_image_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        device_image_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeDeviceImage(DeviceImage obj) {
        if (device_image_refs != null) {
            device_image_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearDeviceImage() {
        if (device_image_refs != null) {
            device_image_refs.clear();
            return;
        }
        device_image_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getLinkAggregationGroups() {
        return link_aggregation_groups;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRole() {
        return physical_role_refs;
    }

    public void setPhysicalRole(PhysicalRole obj) {
        physical_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        physical_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addPhysicalRole(PhysicalRole obj) {
        if (physical_role_refs == null) {
            physical_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        physical_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removePhysicalRole(PhysicalRole obj) {
        if (physical_role_refs != null) {
            physical_role_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearPhysicalRole() {
        if (physical_role_refs != null) {
            physical_role_refs.clear();
            return;
        }
        physical_role_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getOverlayRole() {
        return overlay_role_refs;
    }

    public void setOverlayRole(OverlayRole obj) {
        overlay_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        overlay_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addOverlayRole(OverlayRole obj) {
        if (overlay_role_refs == null) {
            overlay_role_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        overlay_role_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeOverlayRole(OverlayRole obj) {
        if (overlay_role_refs != null) {
            overlay_role_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearOverlayRole() {
        if (overlay_role_refs != null) {
            overlay_role_refs.clear();
            return;
        }
        overlay_role_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getHardwareInventorys() {
        return hardware_inventorys;
    }

    public List<ObjectReference<ApiPropertyBase>> getCliConfigs() {
        return cli_configs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalInterfaces() {
        return physical_interfaces;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalInterfaces() {
        return logical_interfaces;
    }

    public List<ObjectReference<ApiPropertyBase>> getTelemetryProfile() {
        return telemetry_profile_refs;
    }

    public void setTelemetryProfile(TelemetryProfile obj) {
        telemetry_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        telemetry_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addTelemetryProfile(TelemetryProfile obj) {
        if (telemetry_profile_refs == null) {
            telemetry_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        telemetry_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeTelemetryProfile(TelemetryProfile obj) {
        if (telemetry_profile_refs != null) {
            telemetry_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearTelemetryProfile() {
        if (telemetry_profile_refs != null) {
            telemetry_profile_refs.clear();
            return;
        }
        telemetry_profile_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getInstanceIpBackRefs() {
        return instance_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouterBackRefs() {
        return logical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceEndpointBackRefs() {
        return service_endpoint_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetworkDeviceConfigBackRefs() {
        return network_device_config_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getE2ServiceProviderBackRefs() {
        return e2_service_provider_back_refs;
    }
}