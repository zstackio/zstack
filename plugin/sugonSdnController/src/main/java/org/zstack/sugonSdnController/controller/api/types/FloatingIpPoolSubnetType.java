//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FloatingIpPoolSubnetType extends ApiPropertyBase {
    List<String> subnet_uuid;
    public FloatingIpPoolSubnetType() {
    }
    public FloatingIpPoolSubnetType(List<String> subnet_uuid) {
        this.subnet_uuid = subnet_uuid;
    }
    
    public List<String> getSubnetUuid() {
        return subnet_uuid;
    }
    
    
    public void addSubnetUuid(String obj) {
        if (subnet_uuid == null) {
            subnet_uuid = new ArrayList<String>();
        }
        subnet_uuid.add(obj);
    }
    public void clearSubnetUuid() {
        subnet_uuid = null;
    }
    
}
