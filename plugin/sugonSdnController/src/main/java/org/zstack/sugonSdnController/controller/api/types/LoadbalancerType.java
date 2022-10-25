//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LoadbalancerType extends ApiPropertyBase {
    String status;
    String provisioning_status;
    String operating_status;
    String vip_subnet_id;
    String vip_address;
    Boolean admin_state;
    public LoadbalancerType() {
    }
    public LoadbalancerType(String status, String provisioning_status, String operating_status, String vip_subnet_id, String vip_address, Boolean admin_state) {
        this.status = status;
        this.provisioning_status = provisioning_status;
        this.operating_status = operating_status;
        this.vip_subnet_id = vip_subnet_id;
        this.vip_address = vip_address;
        this.admin_state = admin_state;
    }
    public LoadbalancerType(String status) {
        this(status, null, null, null, null, true);    }
    public LoadbalancerType(String status, String provisioning_status) {
        this(status, provisioning_status, null, null, null, true);    }
    public LoadbalancerType(String status, String provisioning_status, String operating_status) {
        this(status, provisioning_status, operating_status, null, null, true);    }
    public LoadbalancerType(String status, String provisioning_status, String operating_status, String vip_subnet_id) {
        this(status, provisioning_status, operating_status, vip_subnet_id, null, true);    }
    public LoadbalancerType(String status, String provisioning_status, String operating_status, String vip_subnet_id, String vip_address) {
        this(status, provisioning_status, operating_status, vip_subnet_id, vip_address, true);    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    
    public String getProvisioningStatus() {
        return provisioning_status;
    }
    
    public void setProvisioningStatus(String provisioning_status) {
        this.provisioning_status = provisioning_status;
    }
    
    
    public String getOperatingStatus() {
        return operating_status;
    }
    
    public void setOperatingStatus(String operating_status) {
        this.operating_status = operating_status;
    }
    
    
    public String getVipSubnetId() {
        return vip_subnet_id;
    }
    
    public void setVipSubnetId(String vip_subnet_id) {
        this.vip_subnet_id = vip_subnet_id;
    }
    
    
    public String getVipAddress() {
        return vip_address;
    }
    
    public void setVipAddress(String vip_address) {
        this.vip_address = vip_address;
    }
    
    
    public Boolean getAdminState() {
        return admin_state;
    }
    
    public void setAdminState(Boolean admin_state) {
        this.admin_state = admin_state;
    }
    
}
