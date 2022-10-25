//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LacpParams extends ApiPropertyBase {
    Boolean lacp_enable;
    String lacp_interval;
    String lacp_mode;
    public LacpParams() {
    }
    public LacpParams(Boolean lacp_enable, String lacp_interval, String lacp_mode) {
        this.lacp_enable = lacp_enable;
        this.lacp_interval = lacp_interval;
        this.lacp_mode = lacp_mode;
    }
    public LacpParams(Boolean lacp_enable) {
        this(lacp_enable, null, null);    }
    public LacpParams(Boolean lacp_enable, String lacp_interval) {
        this(lacp_enable, lacp_interval, null);    }
    
    public Boolean getLacpEnable() {
        return lacp_enable;
    }
    
    public void setLacpEnable(Boolean lacp_enable) {
        this.lacp_enable = lacp_enable;
    }
    
    
    public String getLacpInterval() {
        return lacp_interval;
    }
    
    public void setLacpInterval(String lacp_interval) {
        this.lacp_interval = lacp_interval;
    }
    
    
    public String getLacpMode() {
        return lacp_mode;
    }
    
    public void setLacpMode(String lacp_mode) {
        this.lacp_mode = lacp_mode;
    }
    
}
