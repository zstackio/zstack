//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MatchConditionType extends ApiPropertyBase {
    String protocol;
    AddressType src_address;
    PortType src_port;
    AddressType dst_address;
    PortType dst_port;
    String ethertype;
    public MatchConditionType() {
    }
    public MatchConditionType(String protocol, AddressType src_address, PortType src_port, AddressType dst_address, PortType dst_port, String ethertype) {
        this.protocol = protocol;
        this.src_address = src_address;
        this.src_port = src_port;
        this.dst_address = dst_address;
        this.dst_port = dst_port;
        this.ethertype = ethertype;
    }
    public MatchConditionType(String protocol) {
        this(protocol, null, null, null, null, null);    }
    public MatchConditionType(String protocol, AddressType src_address) {
        this(protocol, src_address, null, null, null, null);    }
    public MatchConditionType(String protocol, AddressType src_address, PortType src_port) {
        this(protocol, src_address, src_port, null, null, null);    }
    public MatchConditionType(String protocol, AddressType src_address, PortType src_port, AddressType dst_address) {
        this(protocol, src_address, src_port, dst_address, null, null);    }
    public MatchConditionType(String protocol, AddressType src_address, PortType src_port, AddressType dst_address, PortType dst_port) {
        this(protocol, src_address, src_port, dst_address, dst_port, null);    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public AddressType getSrcAddress() {
        return src_address;
    }
    
    public void setSrcAddress(AddressType src_address) {
        this.src_address = src_address;
    }
    
    
    public PortType getSrcPort() {
        return src_port;
    }
    
    public void setSrcPort(PortType src_port) {
        this.src_port = src_port;
    }
    
    
    public AddressType getDstAddress() {
        return dst_address;
    }
    
    public void setDstAddress(AddressType dst_address) {
        this.dst_address = dst_address;
    }
    
    
    public PortType getDstPort() {
        return dst_port;
    }
    
    public void setDstPort(PortType dst_port) {
        this.dst_port = dst_port;
    }
    
    
    public String getEthertype() {
        return ethertype;
    }
    
    public void setEthertype(String ethertype) {
        this.ethertype = ethertype;
    }
    
}
