//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FirewallServiceType extends ApiPropertyBase {
    String protocol;
    Integer protocol_id;
    PortType src_ports;
    PortType dst_ports;
    public FirewallServiceType() {
    }
    public FirewallServiceType(String protocol, Integer protocol_id, PortType src_ports, PortType dst_ports) {
        this.protocol = protocol;
        this.protocol_id = protocol_id;
        this.src_ports = src_ports;
        this.dst_ports = dst_ports;
    }
    public FirewallServiceType(String protocol) {
        this(protocol, null, null, null);    }
    public FirewallServiceType(String protocol, Integer protocol_id) {
        this(protocol, protocol_id, null, null);    }
    public FirewallServiceType(String protocol, Integer protocol_id, PortType src_ports) {
        this(protocol, protocol_id, src_ports, null);    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public Integer getProtocolId() {
        return protocol_id;
    }
    
    public void setProtocolId(Integer protocol_id) {
        this.protocol_id = protocol_id;
    }
    
    
    public PortType getSrcPorts() {
        return src_ports;
    }
    
    public void setSrcPorts(PortType src_ports) {
        this.src_ports = src_ports;
    }
    
    
    public PortType getDstPorts() {
        return dst_ports;
    }
    
    public void setDstPorts(PortType dst_ports) {
        this.dst_ports = dst_ports;
    }
    
}
