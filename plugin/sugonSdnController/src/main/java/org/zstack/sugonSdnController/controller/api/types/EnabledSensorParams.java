//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class EnabledSensorParams extends ApiPropertyBase {
    Boolean physical_health;
    Boolean interface_health;
    Boolean control_plane_health;
    Boolean service_layer_health;
    public EnabledSensorParams() {
    }
    public EnabledSensorParams(Boolean physical_health, Boolean interface_health, Boolean control_plane_health, Boolean service_layer_health) {
        this.physical_health = physical_health;
        this.interface_health = interface_health;
        this.control_plane_health = control_plane_health;
        this.service_layer_health = service_layer_health;
    }
    public EnabledSensorParams(Boolean physical_health) {
        this(physical_health, true, true, true);    }
    public EnabledSensorParams(Boolean physical_health, Boolean interface_health) {
        this(physical_health, interface_health, true, true);    }
    public EnabledSensorParams(Boolean physical_health, Boolean interface_health, Boolean control_plane_health) {
        this(physical_health, interface_health, control_plane_health, true);    }
    
    public Boolean getPhysicalHealth() {
        return physical_health;
    }
    
    public void setPhysicalHealth(Boolean physical_health) {
        this.physical_health = physical_health;
    }
    
    
    public Boolean getInterfaceHealth() {
        return interface_health;
    }
    
    public void setInterfaceHealth(Boolean interface_health) {
        this.interface_health = interface_health;
    }
    
    
    public Boolean getControlPlaneHealth() {
        return control_plane_health;
    }
    
    public void setControlPlaneHealth(Boolean control_plane_health) {
        this.control_plane_health = control_plane_health;
    }
    
    
    public Boolean getServiceLayerHealth() {
        return service_layer_health;
    }
    
    public void setServiceLayerHealth(Boolean service_layer_health) {
        this.service_layer_health = service_layer_health;
    }
    
}
