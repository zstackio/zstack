//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class StaticMirrorNhType extends ApiPropertyBase {
    String vtep_dst_ip_address;
    String vtep_dst_mac_address;
    Integer vni;
    public StaticMirrorNhType() {
    }
    public StaticMirrorNhType(String vtep_dst_ip_address, String vtep_dst_mac_address, Integer vni) {
        this.vtep_dst_ip_address = vtep_dst_ip_address;
        this.vtep_dst_mac_address = vtep_dst_mac_address;
        this.vni = vni;
    }
    public StaticMirrorNhType(String vtep_dst_ip_address) {
        this(vtep_dst_ip_address, null, null);    }
    public StaticMirrorNhType(String vtep_dst_ip_address, String vtep_dst_mac_address) {
        this(vtep_dst_ip_address, vtep_dst_mac_address, null);    }
    
    public String getVtepDstIpAddress() {
        return vtep_dst_ip_address;
    }
    
    public void setVtepDstIpAddress(String vtep_dst_ip_address) {
        this.vtep_dst_ip_address = vtep_dst_ip_address;
    }
    
    
    public String getVtepDstMacAddress() {
        return vtep_dst_mac_address;
    }
    
    public void setVtepDstMacAddress(String vtep_dst_mac_address) {
        this.vtep_dst_mac_address = vtep_dst_mac_address;
    }
    
    
    public Integer getVni() {
        return vni;
    }
    
    public void setVni(Integer vni) {
        this.vni = vni;
    }
    
}
