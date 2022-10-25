//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VirtualIpType extends ApiPropertyBase {
    String address;
    String status;
    String status_description;
    Boolean admin_state;
    String protocol;
    Integer protocol_port;
    Integer connection_limit;
    String subnet_id;
    String persistence_cookie_name;
    String persistence_type;
    public VirtualIpType() {
    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state, String protocol, Integer protocol_port, Integer connection_limit, String subnet_id, String persistence_cookie_name, String persistence_type) {
        this.address = address;
        this.status = status;
        this.status_description = status_description;
        this.admin_state = admin_state;
        this.protocol = protocol;
        this.protocol_port = protocol_port;
        this.connection_limit = connection_limit;
        this.subnet_id = subnet_id;
        this.persistence_cookie_name = persistence_cookie_name;
        this.persistence_type = persistence_type;
    }
    public VirtualIpType(String address) {
        this(address, null, null, true, null, null, null, null, null, null);    }
    public VirtualIpType(String address, String status) {
        this(address, status, null, true, null, null, null, null, null, null);    }
    public VirtualIpType(String address, String status, String status_description) {
        this(address, status, status_description, true, null, null, null, null, null, null);    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state) {
        this(address, status, status_description, admin_state, null, null, null, null, null, null);    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state, String protocol) {
        this(address, status, status_description, admin_state, protocol, null, null, null, null, null);    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state, String protocol, Integer protocol_port) {
        this(address, status, status_description, admin_state, protocol, protocol_port, null, null, null, null);    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state, String protocol, Integer protocol_port, Integer connection_limit) {
        this(address, status, status_description, admin_state, protocol, protocol_port, connection_limit, null, null, null);    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state, String protocol, Integer protocol_port, Integer connection_limit, String subnet_id) {
        this(address, status, status_description, admin_state, protocol, protocol_port, connection_limit, subnet_id, null, null);    }
    public VirtualIpType(String address, String status, String status_description, Boolean admin_state, String protocol, Integer protocol_port, Integer connection_limit, String subnet_id, String persistence_cookie_name) {
        this(address, status, status_description, admin_state, protocol, protocol_port, connection_limit, subnet_id, persistence_cookie_name, null);    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    
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
    
    
    public Integer getProtocolPort() {
        return protocol_port;
    }
    
    public void setProtocolPort(Integer protocol_port) {
        this.protocol_port = protocol_port;
    }
    
    
    public Integer getConnectionLimit() {
        return connection_limit;
    }
    
    public void setConnectionLimit(Integer connection_limit) {
        this.connection_limit = connection_limit;
    }
    
    
    public String getSubnetId() {
        return subnet_id;
    }
    
    public void setSubnetId(String subnet_id) {
        this.subnet_id = subnet_id;
    }
    
    
    public String getPersistenceCookieName() {
        return persistence_cookie_name;
    }
    
    public void setPersistenceCookieName(String persistence_cookie_name) {
        this.persistence_cookie_name = persistence_cookie_name;
    }
    
    
    public String getPersistenceType() {
        return persistence_type;
    }
    
    public void setPersistenceType(String persistence_type) {
        this.persistence_type = persistence_type;
    }
    
}
