//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FatFlowProtocols extends ApiPropertyBase {
    List<ProtocolType> fat_flow_protocol;
    public FatFlowProtocols() {
    }
    public FatFlowProtocols(List<ProtocolType> fat_flow_protocol) {
        this.fat_flow_protocol = fat_flow_protocol;
    }
    
    public List<ProtocolType> getFatFlowProtocol() {
        return fat_flow_protocol;
    }
    
    
    public void addFatFlowProtocol(ProtocolType obj) {
        if (fat_flow_protocol == null) {
            fat_flow_protocol = new ArrayList<ProtocolType>();
        }
        fat_flow_protocol.add(obj);
    }
    public void clearFatFlowProtocol() {
        fat_flow_protocol = null;
    }
    
}
