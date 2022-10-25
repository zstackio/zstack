//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class GracefulRestartParametersType extends ApiPropertyBase {
    Boolean enable;
    Integer restart_time;
    Integer long_lived_restart_time;
    Integer end_of_rib_timeout;
    Boolean bgp_helper_enable;
    Boolean xmpp_helper_enable;
    public GracefulRestartParametersType() {
    }
    public GracefulRestartParametersType(Boolean enable, Integer restart_time, Integer long_lived_restart_time, Integer end_of_rib_timeout, Boolean bgp_helper_enable, Boolean xmpp_helper_enable) {
        this.enable = enable;
        this.restart_time = restart_time;
        this.long_lived_restart_time = long_lived_restart_time;
        this.end_of_rib_timeout = end_of_rib_timeout;
        this.bgp_helper_enable = bgp_helper_enable;
        this.xmpp_helper_enable = xmpp_helper_enable;
    }
    public GracefulRestartParametersType(Boolean enable) {
        this(enable, 300, 300, 300, false, false);    }
    public GracefulRestartParametersType(Boolean enable, Integer restart_time) {
        this(enable, restart_time, 300, 300, false, false);    }
    public GracefulRestartParametersType(Boolean enable, Integer restart_time, Integer long_lived_restart_time) {
        this(enable, restart_time, long_lived_restart_time, 300, false, false);    }
    public GracefulRestartParametersType(Boolean enable, Integer restart_time, Integer long_lived_restart_time, Integer end_of_rib_timeout) {
        this(enable, restart_time, long_lived_restart_time, end_of_rib_timeout, false, false);    }
    public GracefulRestartParametersType(Boolean enable, Integer restart_time, Integer long_lived_restart_time, Integer end_of_rib_timeout, Boolean bgp_helper_enable) {
        this(enable, restart_time, long_lived_restart_time, end_of_rib_timeout, bgp_helper_enable, false);    }
    
    public Boolean getEnable() {
        return enable;
    }
    
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }
    
    
    public Integer getRestartTime() {
        return restart_time;
    }
    
    public void setRestartTime(Integer restart_time) {
        this.restart_time = restart_time;
    }
    
    
    public Integer getLongLivedRestartTime() {
        return long_lived_restart_time;
    }
    
    public void setLongLivedRestartTime(Integer long_lived_restart_time) {
        this.long_lived_restart_time = long_lived_restart_time;
    }
    
    
    public Integer getEndOfRibTimeout() {
        return end_of_rib_timeout;
    }
    
    public void setEndOfRibTimeout(Integer end_of_rib_timeout) {
        this.end_of_rib_timeout = end_of_rib_timeout;
    }
    
    
    public Boolean getBgpHelperEnable() {
        return bgp_helper_enable;
    }
    
    public void setBgpHelperEnable(Boolean bgp_helper_enable) {
        this.bgp_helper_enable = bgp_helper_enable;
    }
    
    
    public Boolean getXmppHelperEnable() {
        return xmpp_helper_enable;
    }
    
    public void setXmppHelperEnable(Boolean xmpp_helper_enable) {
        this.xmpp_helper_enable = xmpp_helper_enable;
    }
    
}
