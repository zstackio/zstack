//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DomainLimitsType extends ApiPropertyBase {
    Integer project_limit;
    Integer virtual_network_limit;
    Integer security_group_limit;
    public DomainLimitsType() {
    }
    public DomainLimitsType(Integer project_limit, Integer virtual_network_limit, Integer security_group_limit) {
        this.project_limit = project_limit;
        this.virtual_network_limit = virtual_network_limit;
        this.security_group_limit = security_group_limit;
    }
    public DomainLimitsType(Integer project_limit) {
        this(project_limit, null, null);    }
    public DomainLimitsType(Integer project_limit, Integer virtual_network_limit) {
        this(project_limit, virtual_network_limit, null);    }
    
    public Integer getProjectLimit() {
        return project_limit;
    }
    
    public void setProjectLimit(Integer project_limit) {
        this.project_limit = project_limit;
    }
    
    
    public Integer getVirtualNetworkLimit() {
        return virtual_network_limit;
    }
    
    public void setVirtualNetworkLimit(Integer virtual_network_limit) {
        this.virtual_network_limit = virtual_network_limit;
    }
    
    
    public Integer getSecurityGroupLimit() {
        return security_group_limit;
    }
    
    public void setSecurityGroupLimit(Integer security_group_limit) {
        this.security_group_limit = security_group_limit;
    }
    
}
