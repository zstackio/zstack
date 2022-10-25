//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VendorHardwaresType extends ApiPropertyBase {
    List<String> vendor_hardware;
    public VendorHardwaresType() {
    }
    public VendorHardwaresType(List<String> vendor_hardware) {
        this.vendor_hardware = vendor_hardware;
    }
    
    public List<String> getVendorHardware() {
        return vendor_hardware;
    }
    
    
    public void addVendorHardware(String obj) {
        if (vendor_hardware == null) {
            vendor_hardware = new ArrayList<String>();
        }
        vendor_hardware.add(obj);
    }
    public void clearVendorHardware() {
        vendor_hardware = null;
    }
    
}
