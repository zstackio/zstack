//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AllowedAddressPairs extends ApiPropertyBase {
    List<AllowedAddressPair> allowed_address_pair;
    public AllowedAddressPairs() {
    }
    public AllowedAddressPairs(List<AllowedAddressPair> allowed_address_pair) {
        this.allowed_address_pair = allowed_address_pair;
    }
    
    public List<AllowedAddressPair> getAllowedAddressPair() {
        return allowed_address_pair;
    }
    
    
    public void addAllowedAddressPair(AllowedAddressPair obj) {
        if (allowed_address_pair == null) {
            allowed_address_pair = new ArrayList<AllowedAddressPair>();
        }
        allowed_address_pair.add(obj);
    }
    public void clearAllowedAddressPair() {
        allowed_address_pair = null;
    }
    
    
    public void addAllowedAddressPair(SubnetType ip, String mac, String address_mode) {
        if (allowed_address_pair == null) {
            allowed_address_pair = new ArrayList<AllowedAddressPair>();
        }
        allowed_address_pair.add(new AllowedAddressPair(ip, mac, address_mode));
    }
    
}
