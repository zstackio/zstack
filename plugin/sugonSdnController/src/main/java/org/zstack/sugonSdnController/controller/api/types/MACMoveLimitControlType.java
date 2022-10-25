//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MACMoveLimitControlType extends ApiPropertyBase {
    Integer mac_move_limit;
    Integer mac_move_time_window;
    String mac_move_limit_action;
    public MACMoveLimitControlType() {
    }
    public MACMoveLimitControlType(Integer mac_move_limit, Integer mac_move_time_window, String mac_move_limit_action) {
        this.mac_move_limit = mac_move_limit;
        this.mac_move_time_window = mac_move_time_window;
        this.mac_move_limit_action = mac_move_limit_action;
    }
    public MACMoveLimitControlType(Integer mac_move_limit) {
        this(mac_move_limit, 10, "log");    }
    public MACMoveLimitControlType(Integer mac_move_limit, Integer mac_move_time_window) {
        this(mac_move_limit, mac_move_time_window, "log");    }
    
    public Integer getMacMoveLimit() {
        return mac_move_limit;
    }
    
    public void setMacMoveLimit(Integer mac_move_limit) {
        this.mac_move_limit = mac_move_limit;
    }
    
    
    public Integer getMacMoveTimeWindow() {
        return mac_move_time_window;
    }
    
    public void setMacMoveTimeWindow(Integer mac_move_time_window) {
        this.mac_move_time_window = mac_move_time_window;
    }
    
    
    public String getMacMoveLimitAction() {
        return mac_move_limit_action;
    }
    
    public void setMacMoveLimitAction(String mac_move_limit_action) {
        this.mac_move_limit_action = mac_move_limit_action;
    }
    
}
