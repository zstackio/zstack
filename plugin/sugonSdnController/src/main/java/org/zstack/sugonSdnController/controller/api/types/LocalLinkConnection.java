//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LocalLinkConnection extends ApiPropertyBase {
    String switch_info;
    String port_index;
    String port_id;
    String switch_id;
    public LocalLinkConnection() {
    }
    public LocalLinkConnection(String switch_info, String port_index, String port_id, String switch_id) {
        this.switch_info = switch_info;
        this.port_index = port_index;
        this.port_id = port_id;
        this.switch_id = switch_id;
    }
    public LocalLinkConnection(String switch_info) {
        this(switch_info, null, null, null);    }
    public LocalLinkConnection(String switch_info, String port_index) {
        this(switch_info, port_index, null, null);    }
    public LocalLinkConnection(String switch_info, String port_index, String port_id) {
        this(switch_info, port_index, port_id, null);    }
    
    public String getSwitchInfo() {
        return switch_info;
    }
    
    public void setSwitchInfo(String switch_info) {
        this.switch_info = switch_info;
    }
    
    
    public String getPortIndex() {
        return port_index;
    }
    
    public void setPortIndex(String port_index) {
        this.port_index = port_index;
    }
    
    
    public String getPortId() {
        return port_id;
    }
    
    public void setPortId(String port_id) {
        this.port_id = port_id;
    }
    
    
    public String getSwitchId() {
        return switch_id;
    }
    
    public void setSwitchId(String switch_id) {
        this.switch_id = switch_id;
    }
    
}
