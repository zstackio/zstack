//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ServiceScaleOutType extends ApiPropertyBase {
    Integer max_instances;
    Boolean auto_scale;
    public ServiceScaleOutType() {
    }
    public ServiceScaleOutType(Integer max_instances, Boolean auto_scale) {
        this.max_instances = max_instances;
        this.auto_scale = auto_scale;
    }
    public ServiceScaleOutType(Integer max_instances) {
        this(max_instances, false);    }
    
    public Integer getMaxInstances() {
        return max_instances;
    }
    
    public void setMaxInstances(Integer max_instances) {
        this.max_instances = max_instances;
    }
    
    
    public Boolean getAutoScale() {
        return auto_scale;
    }
    
    public void setAutoScale(Boolean auto_scale) {
        this.auto_scale = auto_scale;
    }
    
}
