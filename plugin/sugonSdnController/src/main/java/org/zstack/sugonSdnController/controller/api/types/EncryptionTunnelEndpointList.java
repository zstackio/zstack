//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class EncryptionTunnelEndpointList extends ApiPropertyBase {
    List<EncryptionTunnelEndpoint> endpoint;
    public EncryptionTunnelEndpointList() {
    }
    public EncryptionTunnelEndpointList(List<EncryptionTunnelEndpoint> endpoint) {
        this.endpoint = endpoint;
    }
    
    public List<EncryptionTunnelEndpoint> getEndpoint() {
        return endpoint;
    }
    
    
    public void addEndpoint(EncryptionTunnelEndpoint obj) {
        if (endpoint == null) {
            endpoint = new ArrayList<EncryptionTunnelEndpoint>();
        }
        endpoint.add(obj);
    }
    public void clearEndpoint() {
        endpoint = null;
    }
    
    
    public void addEndpoint(String tunnel_remote_ip_address) {
        if (endpoint == null) {
            endpoint = new ArrayList<EncryptionTunnelEndpoint>();
        }
        endpoint.add(new EncryptionTunnelEndpoint(tunnel_remote_ip_address));
    }
    
}
