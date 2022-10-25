//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortTranslationPool extends ApiPropertyBase {
    String protocol;
    PortType port_range;
    String port_count;
    public PortTranslationPool() {
    }
    public PortTranslationPool(String protocol, PortType port_range, String port_count) {
        this.protocol = protocol;
        this.port_range = port_range;
        this.port_count = port_count;
    }
    public PortTranslationPool(String protocol) {
        this(protocol, null, null);    }
    public PortTranslationPool(String protocol, PortType port_range) {
        this(protocol, port_range, null);    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public PortType getPortRange() {
        return port_range;
    }
    
    public void setPortRange(PortType port_range) {
        this.port_range = port_range;
    }
    
    
    public String getPortCount() {
        return port_count;
    }
    
    public void setPortCount(String port_count) {
        this.port_count = port_count;
    }
    
}
