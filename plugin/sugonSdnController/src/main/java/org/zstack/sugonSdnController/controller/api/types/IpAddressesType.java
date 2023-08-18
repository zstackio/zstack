//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class IpAddressesType extends ApiPropertyBase {
    List<String> ip_address;
    public IpAddressesType() {
    }
    public IpAddressesType(List<String> ip_address) {
        this.ip_address = ip_address;
    }
    
    public List<String> getIpAddress() {
        return ip_address;
    }
    
    
    public void addIpAddress(String obj) {
        if (ip_address == null) {
            ip_address = new ArrayList<String>();
        }
        ip_address.add(obj);
    }
    public void clearIpAddress() {
        ip_address = null;
    }
    
}
