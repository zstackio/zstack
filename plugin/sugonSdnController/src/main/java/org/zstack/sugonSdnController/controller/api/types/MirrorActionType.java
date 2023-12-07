//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MirrorActionType extends ApiPropertyBase {
    String analyzer_name;
    String encapsulation;
    String analyzer_ip_address;
    String analyzer_mac_address;
    String routing_instance;
    Integer udp_port;
    Boolean juniper_header;
    String nh_mode;
    StaticMirrorNhType static_nh_header;
    Boolean nic_assisted_mirroring;
    Integer nic_assisted_mirroring_vlan;
    public MirrorActionType() {
    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance, Integer udp_port, Boolean juniper_header, String nh_mode, StaticMirrorNhType static_nh_header, Boolean nic_assisted_mirroring, Integer nic_assisted_mirroring_vlan) {
        this.analyzer_name = analyzer_name;
        this.encapsulation = encapsulation;
        this.analyzer_ip_address = analyzer_ip_address;
        this.analyzer_mac_address = analyzer_mac_address;
        this.routing_instance = routing_instance;
        this.udp_port = udp_port;
        this.juniper_header = juniper_header;
        this.nh_mode = nh_mode;
        this.static_nh_header = static_nh_header;
        this.nic_assisted_mirroring = nic_assisted_mirroring;
        this.nic_assisted_mirroring_vlan = nic_assisted_mirroring_vlan;
    }
    public MirrorActionType(String analyzer_name) {
        this(analyzer_name, null, null, null, null, null, true, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation) {
        this(analyzer_name, encapsulation, null, null, null, null, true, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address) {
        this(analyzer_name, encapsulation, analyzer_ip_address, null, null, null, true, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, null, null, true, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, routing_instance, null, true, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance, Integer udp_port) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, routing_instance, udp_port, true, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance, Integer udp_port, Boolean juniper_header) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, routing_instance, udp_port, juniper_header, null, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance, Integer udp_port, Boolean juniper_header, String nh_mode) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, routing_instance, udp_port, juniper_header, nh_mode, null, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance, Integer udp_port, Boolean juniper_header, String nh_mode, StaticMirrorNhType static_nh_header) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, routing_instance, udp_port, juniper_header, nh_mode, static_nh_header, false, null);    }
    public MirrorActionType(String analyzer_name, String encapsulation, String analyzer_ip_address, String analyzer_mac_address, String routing_instance, Integer udp_port, Boolean juniper_header, String nh_mode, StaticMirrorNhType static_nh_header, Boolean nic_assisted_mirroring) {
        this(analyzer_name, encapsulation, analyzer_ip_address, analyzer_mac_address, routing_instance, udp_port, juniper_header, nh_mode, static_nh_header, nic_assisted_mirroring, null);    }
    
    public String getAnalyzerName() {
        return analyzer_name;
    }
    
    public void setAnalyzerName(String analyzer_name) {
        this.analyzer_name = analyzer_name;
    }
    
    
    public String getEncapsulation() {
        return encapsulation;
    }
    
    public void setEncapsulation(String encapsulation) {
        this.encapsulation = encapsulation;
    }
    
    
    public String getAnalyzerIpAddress() {
        return analyzer_ip_address;
    }
    
    public void setAnalyzerIpAddress(String analyzer_ip_address) {
        this.analyzer_ip_address = analyzer_ip_address;
    }
    
    
    public String getAnalyzerMacAddress() {
        return analyzer_mac_address;
    }
    
    public void setAnalyzerMacAddress(String analyzer_mac_address) {
        this.analyzer_mac_address = analyzer_mac_address;
    }
    
    
    public String getRoutingInstance() {
        return routing_instance;
    }
    
    public void setRoutingInstance(String routing_instance) {
        this.routing_instance = routing_instance;
    }
    
    
    public Integer getUdpPort() {
        return udp_port;
    }
    
    public void setUdpPort(Integer udp_port) {
        this.udp_port = udp_port;
    }
    
    
    public Boolean getJuniperHeader() {
        return juniper_header;
    }
    
    public void setJuniperHeader(Boolean juniper_header) {
        this.juniper_header = juniper_header;
    }
    
    
    public String getNhMode() {
        return nh_mode;
    }
    
    public void setNhMode(String nh_mode) {
        this.nh_mode = nh_mode;
    }
    
    
    public StaticMirrorNhType getStaticNhHeader() {
        return static_nh_header;
    }
    
    public void setStaticNhHeader(StaticMirrorNhType static_nh_header) {
        this.static_nh_header = static_nh_header;
    }
    
    
    public Boolean getNicAssistedMirroring() {
        return nic_assisted_mirroring;
    }
    
    public void setNicAssistedMirroring(Boolean nic_assisted_mirroring) {
        this.nic_assisted_mirroring = nic_assisted_mirroring;
    }
    
    
    public Integer getNicAssistedMirroringVlan() {
        return nic_assisted_mirroring_vlan;
    }
    
    public void setNicAssistedMirroringVlan(Integer nic_assisted_mirroring_vlan) {
        this.nic_assisted_mirroring_vlan = nic_assisted_mirroring_vlan;
    }
    
}
