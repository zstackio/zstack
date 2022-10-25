//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ControlTrafficDscpType extends ApiPropertyBase {
    Integer control;
    Integer analytics;
    Integer dns;
    public ControlTrafficDscpType() {
    }
    public ControlTrafficDscpType(Integer control, Integer analytics, Integer dns) {
        this.control = control;
        this.analytics = analytics;
        this.dns = dns;
    }
    public ControlTrafficDscpType(Integer control) {
        this(control, null, null);    }
    public ControlTrafficDscpType(Integer control, Integer analytics) {
        this(control, analytics, null);    }
    
    public Integer getControl() {
        return control;
    }
    
    public void setControl(Integer control) {
        this.control = control;
    }
    
    
    public Integer getAnalytics() {
        return analytics;
    }
    
    public void setAnalytics(Integer analytics) {
        this.analytics = analytics;
    }
    
    
    public Integer getDns() {
        return dns;
    }
    
    public void setDns(Integer dns) {
        this.dns = dns;
    }
    
}
