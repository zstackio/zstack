//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class BaremetalPortGroupInfo extends ApiPropertyBase {
    Boolean standalone_ports_supported;
    String node_uuid;
    PortGroupProperties properties;
    String address;
    String mode;
    public BaremetalPortGroupInfo() {
    }
    public BaremetalPortGroupInfo(Boolean standalone_ports_supported, String node_uuid, PortGroupProperties properties, String address, String mode) {
        this.standalone_ports_supported = standalone_ports_supported;
        this.node_uuid = node_uuid;
        this.properties = properties;
        this.address = address;
        this.mode = mode;
    }
    public BaremetalPortGroupInfo(Boolean standalone_ports_supported) {
        this(standalone_ports_supported, null, null, null, null);    }
    public BaremetalPortGroupInfo(Boolean standalone_ports_supported, String node_uuid) {
        this(standalone_ports_supported, node_uuid, null, null, null);    }
    public BaremetalPortGroupInfo(Boolean standalone_ports_supported, String node_uuid, PortGroupProperties properties) {
        this(standalone_ports_supported, node_uuid, properties, null, null);    }
    public BaremetalPortGroupInfo(Boolean standalone_ports_supported, String node_uuid, PortGroupProperties properties, String address) {
        this(standalone_ports_supported, node_uuid, properties, address, null);    }
    
    public Boolean getStandalonePortsSupported() {
        return standalone_ports_supported;
    }
    
    public void setStandalonePortsSupported(Boolean standalone_ports_supported) {
        this.standalone_ports_supported = standalone_ports_supported;
    }
    
    
    public String getNodeUuid() {
        return node_uuid;
    }
    
    public void setNodeUuid(String node_uuid) {
        this.node_uuid = node_uuid;
    }
    
    
    public PortGroupProperties getProperties() {
        return properties;
    }
    
    public void setProperties(PortGroupProperties properties) {
        this.properties = properties;
    }
    
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
}
