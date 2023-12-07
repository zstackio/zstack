//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PolicyBasedForwardingRuleType extends ApiPropertyBase {
    String direction;
    Integer vlan_tag;
    String src_mac;
    String dst_mac;
    Integer mpls_label;
    String service_chain_address;
    String ipv6_service_chain_address;
    String protocol;
    public PolicyBasedForwardingRuleType() {
    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag, String src_mac, String dst_mac, Integer mpls_label, String service_chain_address, String ipv6_service_chain_address, String protocol) {
        this.direction = direction;
        this.vlan_tag = vlan_tag;
        this.src_mac = src_mac;
        this.dst_mac = dst_mac;
        this.mpls_label = mpls_label;
        this.service_chain_address = service_chain_address;
        this.ipv6_service_chain_address = ipv6_service_chain_address;
        this.protocol = protocol;
    }
    public PolicyBasedForwardingRuleType(String direction) {
        this(direction, null, null, null, null, null, null, null);    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag) {
        this(direction, vlan_tag, null, null, null, null, null, null);    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag, String src_mac) {
        this(direction, vlan_tag, src_mac, null, null, null, null, null);    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag, String src_mac, String dst_mac) {
        this(direction, vlan_tag, src_mac, dst_mac, null, null, null, null);    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag, String src_mac, String dst_mac, Integer mpls_label) {
        this(direction, vlan_tag, src_mac, dst_mac, mpls_label, null, null, null);    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag, String src_mac, String dst_mac, Integer mpls_label, String service_chain_address) {
        this(direction, vlan_tag, src_mac, dst_mac, mpls_label, service_chain_address, null, null);    }
    public PolicyBasedForwardingRuleType(String direction, Integer vlan_tag, String src_mac, String dst_mac, Integer mpls_label, String service_chain_address, String ipv6_service_chain_address) {
        this(direction, vlan_tag, src_mac, dst_mac, mpls_label, service_chain_address, ipv6_service_chain_address, null);    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    
    public Integer getVlanTag() {
        return vlan_tag;
    }
    
    public void setVlanTag(Integer vlan_tag) {
        this.vlan_tag = vlan_tag;
    }
    
    
    public String getSrcMac() {
        return src_mac;
    }
    
    public void setSrcMac(String src_mac) {
        this.src_mac = src_mac;
    }
    
    
    public String getDstMac() {
        return dst_mac;
    }
    
    public void setDstMac(String dst_mac) {
        this.dst_mac = dst_mac;
    }
    
    
    public Integer getMplsLabel() {
        return mpls_label;
    }
    
    public void setMplsLabel(Integer mpls_label) {
        this.mpls_label = mpls_label;
    }
    
    
    public String getServiceChainAddress() {
        return service_chain_address;
    }
    
    public void setServiceChainAddress(String service_chain_address) {
        this.service_chain_address = service_chain_address;
    }
    
    
    public String getIpv6ServiceChainAddress() {
        return ipv6_service_chain_address;
    }
    
    public void setIpv6ServiceChainAddress(String ipv6_service_chain_address) {
        this.ipv6_service_chain_address = ipv6_service_chain_address;
    }
    
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
}
