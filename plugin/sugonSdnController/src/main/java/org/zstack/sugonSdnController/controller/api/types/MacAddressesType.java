//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MacAddressesType extends ApiPropertyBase {
    List<String> mac_address;
    public MacAddressesType() {
    }
    public MacAddressesType(List<String> mac_address) {
        this.mac_address = mac_address;
    }
    
    public List<String> getMacAddress() {
        return mac_address;
    }
    
    
    public void addMacAddress(String obj) {
        if (mac_address == null) {
            mac_address = new ArrayList<String>();
        }
        mac_address.add(obj);
    }
    public void clearMacAddress() {
        mac_address = null;
    }
    
}
