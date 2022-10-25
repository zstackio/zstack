//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class NetconfParameters extends ApiPropertyBase {
    EnabledSensorParams enabled_sensor_params;
    public NetconfParameters() {
    }
    public NetconfParameters(EnabledSensorParams enabled_sensor_params) {
        this.enabled_sensor_params = enabled_sensor_params;
    }
    
    public EnabledSensorParams getEnabledSensorParams() {
        return enabled_sensor_params;
    }
    
    public void setEnabledSensorParams(EnabledSensorParams enabled_sensor_params) {
        this.enabled_sensor_params = enabled_sensor_params;
    }
    
}
