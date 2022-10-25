//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ServiceTemplateInterfaceType extends ApiPropertyBase {
    String service_interface_type;
    Boolean shared_ip;
    Boolean static_route_enable;
    public ServiceTemplateInterfaceType() {
    }
    public ServiceTemplateInterfaceType(String service_interface_type, Boolean shared_ip, Boolean static_route_enable) {
        this.service_interface_type = service_interface_type;
        this.shared_ip = shared_ip;
        this.static_route_enable = static_route_enable;
    }
    public ServiceTemplateInterfaceType(String service_interface_type) {
        this(service_interface_type, false, false);    }
    public ServiceTemplateInterfaceType(String service_interface_type, Boolean shared_ip) {
        this(service_interface_type, shared_ip, false);    }
    
    public String getServiceInterfaceType() {
        return service_interface_type;
    }
    
    public void setServiceInterfaceType(String service_interface_type) {
        this.service_interface_type = service_interface_type;
    }
    
    
    public Boolean getSharedIp() {
        return shared_ip;
    }
    
    public void setSharedIp(Boolean shared_ip) {
        this.shared_ip = shared_ip;
    }
    
    
    public Boolean getStaticRouteEnable() {
        return static_route_enable;
    }
    
    public void setStaticRouteEnable(Boolean static_route_enable) {
        this.static_route_enable = static_route_enable;
    }
    
}
