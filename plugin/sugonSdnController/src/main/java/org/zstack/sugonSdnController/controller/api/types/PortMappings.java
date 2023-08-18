//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortMappings extends ApiPropertyBase {
    List<PortMap> port_mappings;
    public PortMappings() {
    }
    public PortMappings(List<PortMap> port_mappings) {
        this.port_mappings = port_mappings;
    }
    
    public List<PortMap> getPortMappings() {
        return port_mappings;
    }
    
    
    public void addPortMappings(PortMap obj) {
        if (port_mappings == null) {
            port_mappings = new ArrayList<PortMap>();
        }
        port_mappings.add(obj);
    }
    public void clearPortMappings() {
        port_mappings = null;
    }
    
    
    public void addPortMappings(String protocol, Integer src_port, Integer dst_port) {
        if (port_mappings == null) {
            port_mappings = new ArrayList<PortMap>();
        }
        port_mappings.add(new PortMap(protocol, src_port, dst_port));
    }
    
}
