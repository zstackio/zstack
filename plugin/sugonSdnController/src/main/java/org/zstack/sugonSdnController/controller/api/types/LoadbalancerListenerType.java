//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LoadbalancerListenerType extends ApiPropertyBase {
    String protocol;
    Integer protocol_port;
    Boolean admin_state;
    Integer connection_limit;
    String default_tls_container;
    List<String> sni_containers;
    public LoadbalancerListenerType() {
    }
    public LoadbalancerListenerType(String protocol, Integer protocol_port, Boolean admin_state, Integer connection_limit, String default_tls_container, List<String> sni_containers) {
        this.protocol = protocol;
        this.protocol_port = protocol_port;
        this.admin_state = admin_state;
        this.connection_limit = connection_limit;
        this.default_tls_container = default_tls_container;
        this.sni_containers = sni_containers;
    }
    public LoadbalancerListenerType(String protocol) {
        this(protocol, null, true, null, null, null);    }
    public LoadbalancerListenerType(String protocol, Integer protocol_port) {
        this(protocol, protocol_port, true, null, null, null);    }
    public LoadbalancerListenerType(String protocol, Integer protocol_port, Boolean admin_state) {
        this(protocol, protocol_port, admin_state, null, null, null);    }
    public LoadbalancerListenerType(String protocol, Integer protocol_port, Boolean admin_state, Integer connection_limit) {
        this(protocol, protocol_port, admin_state, connection_limit, null, null);    }
    public LoadbalancerListenerType(String protocol, Integer protocol_port, Boolean admin_state, Integer connection_limit, String default_tls_container) {
        this(protocol, protocol_port, admin_state, connection_limit, default_tls_container, null);    }
    
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
    
    
    public Boolean getAdminState() {
        return admin_state;
    }
    
    public void setAdminState(Boolean admin_state) {
        this.admin_state = admin_state;
    }
    
    
    public Integer getConnectionLimit() {
        return connection_limit;
    }
    
    public void setConnectionLimit(Integer connection_limit) {
        this.connection_limit = connection_limit;
    }
    
    
    public String getDefaultTlsContainer() {
        return default_tls_container;
    }
    
    public void setDefaultTlsContainer(String default_tls_container) {
        this.default_tls_container = default_tls_container;
    }
    
    
    public List<String> getSniContainers() {
        return sni_containers;
    }
    
    
    public void addSniContainers(String obj) {
        if (sni_containers == null) {
            sni_containers = new ArrayList<String>();
        }
        sni_containers.add(obj);
    }
    public void clearSniContainers() {
        sni_containers = null;
    }
    
}
