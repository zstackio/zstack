//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FirewallServiceGroupType extends ApiPropertyBase {
    List<FirewallServiceType> firewall_service;
    public FirewallServiceGroupType() {
    }
    public FirewallServiceGroupType(List<FirewallServiceType> firewall_service) {
        this.firewall_service = firewall_service;
    }
    
    public List<FirewallServiceType> getFirewallService() {
        return firewall_service;
    }
    
    
    public void addFirewallService(FirewallServiceType obj) {
        if (firewall_service == null) {
            firewall_service = new ArrayList<FirewallServiceType>();
        }
        firewall_service.add(obj);
    }
    public void clearFirewallService() {
        firewall_service = null;
    }
    
    
    public void addFirewallService(String protocol, Integer protocol_id, PortType src_ports, PortType dst_ports) {
        if (firewall_service == null) {
            firewall_service = new ArrayList<FirewallServiceType>();
        }
        firewall_service.add(new FirewallServiceType(protocol, protocol_id, src_ports, dst_ports));
    }
    
}
