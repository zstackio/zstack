//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class BfdParameters extends ApiPropertyBase {
    Integer time_interval;
    Integer detection_time_multiplier;
    public BfdParameters() {
    }
    public BfdParameters(Integer time_interval, Integer detection_time_multiplier) {
        this.time_interval = time_interval;
        this.detection_time_multiplier = detection_time_multiplier;
    }
    public BfdParameters(Integer time_interval) {
        this(time_interval, null);    }
    
    public Integer getTimeInterval() {
        return time_interval;
    }
    
    public void setTimeInterval(Integer time_interval) {
        this.time_interval = time_interval;
    }
    
    
    public Integer getDetectionTimeMultiplier() {
        return detection_time_multiplier;
    }
    
    public void setDetectionTimeMultiplier(Integer detection_time_multiplier) {
        this.detection_time_multiplier = detection_time_multiplier;
    }
    
}
