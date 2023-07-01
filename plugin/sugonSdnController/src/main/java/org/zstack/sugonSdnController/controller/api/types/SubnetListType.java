//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SubnetListType extends ApiPropertyBase {
    List<SubnetType> subnet;
    public SubnetListType() {
    }
    public SubnetListType(List<SubnetType> subnet) {
        this.subnet = subnet;
    }
    
    public List<SubnetType> getSubnet() {
        return subnet;
    }
    
    
    public void addSubnet(SubnetType obj) {
        if (subnet == null) {
            subnet = new ArrayList<SubnetType>();
        }
        subnet.add(obj);
    }
    public void clearSubnet() {
        subnet = null;
    }
    
    
    public void addSubnet(String ip_prefix, Integer ip_prefix_len) {
        if (subnet == null) {
            subnet = new ArrayList<SubnetType>();
        }
        subnet.add(new SubnetType(ip_prefix, ip_prefix_len));
    }
    
}
