//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VirtualNetworkType extends ApiPropertyBase {
    Boolean allow_transit;
    Integer network_id;
    Integer vxlan_network_identifier;
    String forwarding_mode;
    String rpf;
    Boolean mirror_destination;
    Integer max_flows;
    public VirtualNetworkType() {
    }
    public VirtualNetworkType(Boolean allow_transit, Integer network_id, Integer vxlan_network_identifier, String forwarding_mode, String rpf, Boolean mirror_destination, Integer max_flows) {
        this.allow_transit = allow_transit;
        this.network_id = network_id;
        this.vxlan_network_identifier = vxlan_network_identifier;
        this.forwarding_mode = forwarding_mode;
        this.rpf = rpf;
        this.mirror_destination = mirror_destination;
        this.max_flows = max_flows;
    }
    public VirtualNetworkType(Boolean allow_transit) {
        this(allow_transit, null, null, null, null, false, 0);    }
    public VirtualNetworkType(Boolean allow_transit, Integer network_id) {
        this(allow_transit, network_id, null, null, null, false, 0);    }
    public VirtualNetworkType(Boolean allow_transit, Integer network_id, Integer vxlan_network_identifier) {
        this(allow_transit, network_id, vxlan_network_identifier, null, null, false, 0);    }
    public VirtualNetworkType(Boolean allow_transit, Integer network_id, Integer vxlan_network_identifier, String forwarding_mode) {
        this(allow_transit, network_id, vxlan_network_identifier, forwarding_mode, null, false, 0);    }
    public VirtualNetworkType(Boolean allow_transit, Integer network_id, Integer vxlan_network_identifier, String forwarding_mode, String rpf) {
        this(allow_transit, network_id, vxlan_network_identifier, forwarding_mode, rpf, false, 0);    }
    public VirtualNetworkType(Boolean allow_transit, Integer network_id, Integer vxlan_network_identifier, String forwarding_mode, String rpf, Boolean mirror_destination) {
        this(allow_transit, network_id, vxlan_network_identifier, forwarding_mode, rpf, mirror_destination, 0);    }
    
    public Boolean getAllowTransit() {
        return allow_transit;
    }
    
    public void setAllowTransit(Boolean allow_transit) {
        this.allow_transit = allow_transit;
    }
    
    
    public Integer getNetworkId() {
        return network_id;
    }
    
    public void setNetworkId(Integer network_id) {
        this.network_id = network_id;
    }
    
    
    public Integer getVxlanNetworkIdentifier() {
        return vxlan_network_identifier;
    }
    
    public void setVxlanNetworkIdentifier(Integer vxlan_network_identifier) {
        this.vxlan_network_identifier = vxlan_network_identifier;
    }
    
    
    public String getForwardingMode() {
        return forwarding_mode;
    }
    
    public void setForwardingMode(String forwarding_mode) {
        this.forwarding_mode = forwarding_mode;
    }
    
    
    public String getRpf() {
        return rpf;
    }
    
    public void setRpf(String rpf) {
        this.rpf = rpf;
    }
    
    
    public Boolean getMirrorDestination() {
        return mirror_destination;
    }
    
    public void setMirrorDestination(Boolean mirror_destination) {
        this.mirror_destination = mirror_destination;
    }
    
    
    public Integer getMaxFlows() {
        return max_flows;
    }
    
    public void setMaxFlows(Integer max_flows) {
        this.max_flows = max_flows;
    }
    
}
