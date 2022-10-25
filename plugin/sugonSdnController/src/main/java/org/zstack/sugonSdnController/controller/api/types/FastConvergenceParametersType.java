//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FastConvergenceParametersType extends ApiPropertyBase {
    Boolean enable;
    Boolean nh_reachability_check;
    Integer xmpp_hold_time;
    public FastConvergenceParametersType() {
    }
    public FastConvergenceParametersType(Boolean enable, Boolean nh_reachability_check, Integer xmpp_hold_time) {
        this.enable = enable;
        this.nh_reachability_check = nh_reachability_check;
        this.xmpp_hold_time = xmpp_hold_time;
    }
    public FastConvergenceParametersType(Boolean enable) {
        this(enable, false, 90);    }
    public FastConvergenceParametersType(Boolean enable, Boolean nh_reachability_check) {
        this(enable, nh_reachability_check, 90);    }
    
    public Boolean getEnable() {
        return enable;
    }
    
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }
    
    
    public Boolean getNhReachabilityCheck() {
        return nh_reachability_check;
    }
    
    public void setNhReachabilityCheck(Boolean nh_reachability_check) {
        this.nh_reachability_check = nh_reachability_check;
    }
    
    
    public Integer getXmppHoldTime() {
        return xmpp_hold_time;
    }
    
    public void setXmppHoldTime(Integer xmpp_hold_time) {
        this.xmpp_hold_time = xmpp_hold_time;
    }
    
}
