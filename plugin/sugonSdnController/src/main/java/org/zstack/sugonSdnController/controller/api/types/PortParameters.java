//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortParameters extends ApiPropertyBase {
    Boolean port_disable;
    Integer port_mtu;
    String port_description;
    public PortParameters() {
    }
    public PortParameters(Boolean port_disable, Integer port_mtu, String port_description) {
        this.port_disable = port_disable;
        this.port_mtu = port_mtu;
        this.port_description = port_description;
    }
    public PortParameters(Boolean port_disable) {
        this(port_disable, null, null);    }
    public PortParameters(Boolean port_disable, Integer port_mtu) {
        this(port_disable, port_mtu, null);    }
    
    public Boolean getPortDisable() {
        return port_disable;
    }
    
    public void setPortDisable(Boolean port_disable) {
        this.port_disable = port_disable;
    }
    
    
    public Integer getPortMtu() {
        return port_mtu;
    }
    
    public void setPortMtu(Integer port_mtu) {
        this.port_mtu = port_mtu;
    }
    
    
    public String getPortDescription() {
        return port_description;
    }
    
    public void setPortDescription(String port_description) {
        this.port_description = port_description;
    }
    
}
