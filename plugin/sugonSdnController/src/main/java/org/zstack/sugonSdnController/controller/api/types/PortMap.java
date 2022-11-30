//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortMap extends ApiPropertyBase {
    String protocol;
    Integer src_port;
    Integer dst_port;
    public PortMap() {
    }
    public PortMap(String protocol, Integer src_port, Integer dst_port) {
        this.protocol = protocol;
        this.src_port = src_port;
        this.dst_port = dst_port;
    }
    public PortMap(String protocol) {
        this(protocol, null, null);    }
    public PortMap(String protocol, Integer src_port) {
        this(protocol, src_port, null);    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public Integer getSrcPort() {
        return src_port;
    }
    
    public void setSrcPort(Integer src_port) {
        this.src_port = src_port;
    }
    
    
    public Integer getDstPort() {
        return dst_port;
    }
    
    public void setDstPort(Integer dst_port) {
        this.dst_port = dst_port;
    }
    
}
