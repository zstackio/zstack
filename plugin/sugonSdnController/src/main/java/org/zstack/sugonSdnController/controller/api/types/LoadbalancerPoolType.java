//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LoadbalancerPoolType extends ApiPropertyBase {
    String status;
    String status_description;
    Boolean admin_state;
    String protocol;
    String loadbalancer_method;
    String subnet_id;
    String session_persistence;
    String persistence_cookie_name;
    public LoadbalancerPoolType() {
    }
    public LoadbalancerPoolType(String status, String status_description, Boolean admin_state, String protocol, String loadbalancer_method, String subnet_id, String session_persistence, String persistence_cookie_name) {
        this.status = status;
        this.status_description = status_description;
        this.admin_state = admin_state;
        this.protocol = protocol;
        this.loadbalancer_method = loadbalancer_method;
        this.subnet_id = subnet_id;
        this.session_persistence = session_persistence;
        this.persistence_cookie_name = persistence_cookie_name;
    }
    public LoadbalancerPoolType(String status) {
        this(status, null, true, null, null, null, null, null);    }
    public LoadbalancerPoolType(String status, String status_description) {
        this(status, status_description, true, null, null, null, null, null);    }
    public LoadbalancerPoolType(String status, String status_description, Boolean admin_state) {
        this(status, status_description, admin_state, null, null, null, null, null);    }
    public LoadbalancerPoolType(String status, String status_description, Boolean admin_state, String protocol) {
        this(status, status_description, admin_state, protocol, null, null, null, null);    }
    public LoadbalancerPoolType(String status, String status_description, Boolean admin_state, String protocol, String loadbalancer_method) {
        this(status, status_description, admin_state, protocol, loadbalancer_method, null, null, null);    }
    public LoadbalancerPoolType(String status, String status_description, Boolean admin_state, String protocol, String loadbalancer_method, String subnet_id) {
        this(status, status_description, admin_state, protocol, loadbalancer_method, subnet_id, null, null);    }
    public LoadbalancerPoolType(String status, String status_description, Boolean admin_state, String protocol, String loadbalancer_method, String subnet_id, String session_persistence) {
        this(status, status_description, admin_state, protocol, loadbalancer_method, subnet_id, session_persistence, null);    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    
    public String getStatusDescription() {
        return status_description;
    }
    
    public void setStatusDescription(String status_description) {
        this.status_description = status_description;
    }
    
    
    public Boolean getAdminState() {
        return admin_state;
    }
    
    public void setAdminState(Boolean admin_state) {
        this.admin_state = admin_state;
    }
    
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public String getLoadbalancerMethod() {
        return loadbalancer_method;
    }
    
    public void setLoadbalancerMethod(String loadbalancer_method) {
        this.loadbalancer_method = loadbalancer_method;
    }
    
    
    public String getSubnetId() {
        return subnet_id;
    }
    
    public void setSubnetId(String subnet_id) {
        this.subnet_id = subnet_id;
    }
    
    
    public String getSessionPersistence() {
        return session_persistence;
    }
    
    public void setSessionPersistence(String session_persistence) {
        this.session_persistence = session_persistence;
    }
    
    
    public String getPersistenceCookieName() {
        return persistence_cookie_name;
    }
    
    public void setPersistenceCookieName(String persistence_cookie_name) {
        this.persistence_cookie_name = persistence_cookie_name;
    }
    
}
