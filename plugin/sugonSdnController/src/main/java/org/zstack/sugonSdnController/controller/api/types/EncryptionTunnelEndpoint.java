//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class EncryptionTunnelEndpoint extends ApiPropertyBase {
    String tunnel_remote_ip_address;
    public EncryptionTunnelEndpoint() {
    }
    public EncryptionTunnelEndpoint(String tunnel_remote_ip_address) {
        this.tunnel_remote_ip_address = tunnel_remote_ip_address;
    }
    
    public String getTunnelRemoteIpAddress() {
        return tunnel_remote_ip_address;
    }
    
    public void setTunnelRemoteIpAddress(String tunnel_remote_ip_address) {
        this.tunnel_remote_ip_address = tunnel_remote_ip_address;
    }
    
}
