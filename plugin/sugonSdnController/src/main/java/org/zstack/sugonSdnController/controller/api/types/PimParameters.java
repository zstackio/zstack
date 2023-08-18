//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PimParameters extends ApiPropertyBase {
    List<String> rp_ip_address;
    String mode;
    Boolean enable_all_interfaces;
    public PimParameters() {
    }
    public PimParameters(List<String> rp_ip_address, String mode, Boolean enable_all_interfaces) {
        this.rp_ip_address = rp_ip_address;
        this.mode = mode;
        this.enable_all_interfaces = enable_all_interfaces;
    }
    public PimParameters(List<String> rp_ip_address) {
        this(rp_ip_address, "sparse-dense", null);    }
    public PimParameters(List<String> rp_ip_address, String mode) {
        this(rp_ip_address, mode, null);    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    
    public Boolean getEnableAllInterfaces() {
        return enable_all_interfaces;
    }
    
    public void setEnableAllInterfaces(Boolean enable_all_interfaces) {
        this.enable_all_interfaces = enable_all_interfaces;
    }
    
    
    public List<String> getRpIpAddress() {
        return rp_ip_address;
    }
    
    
    public void addRpIpAddress(String obj) {
        if (rp_ip_address == null) {
            rp_ip_address = new ArrayList<String>();
        }
        rp_ip_address.add(obj);
    }
    public void clearRpIpAddress() {
        rp_ip_address = null;
    }
    
}
