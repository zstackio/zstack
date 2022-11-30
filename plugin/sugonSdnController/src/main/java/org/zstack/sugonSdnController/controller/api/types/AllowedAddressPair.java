//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AllowedAddressPair extends ApiPropertyBase {
    SubnetType ip;
    String mac;
    String address_mode;
    public AllowedAddressPair() {
    }
    public AllowedAddressPair(SubnetType ip, String mac, String address_mode) {
        this.ip = ip;
        this.mac = mac;
        this.address_mode = address_mode;
    }
    public AllowedAddressPair(SubnetType ip) {
        this(ip, null, null);    }
    public AllowedAddressPair(SubnetType ip, String mac) {
        this(ip, mac, null);    }
    
    public SubnetType getIp() {
        return ip;
    }
    
    public void setIp(SubnetType ip) {
        this.ip = ip;
    }
    
    
    public String getMac() {
        return mac;
    }
    
    public void setMac(String mac) {
        this.mac = mac;
    }
    
    
    public String getAddressMode() {
        return address_mode;
    }
    
    public void setAddressMode(String address_mode) {
        this.address_mode = address_mode;
    }
    
}
