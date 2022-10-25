//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MACLimitControlType extends ApiPropertyBase {
    Integer mac_limit;
    String mac_limit_action;
    public MACLimitControlType() {
    }
    public MACLimitControlType(Integer mac_limit, String mac_limit_action) {
        this.mac_limit = mac_limit;
        this.mac_limit_action = mac_limit_action;
    }
    public MACLimitControlType(Integer mac_limit) {
        this(mac_limit, "log");    }
    
    public Integer getMacLimit() {
        return mac_limit;
    }
    
    public void setMacLimit(Integer mac_limit) {
        this.mac_limit = mac_limit;
    }
    
    
    public String getMacLimitAction() {
        return mac_limit_action;
    }
    
    public void setMacLimitAction(String mac_limit_action) {
        this.mac_limit_action = mac_limit_action;
    }
    
}
