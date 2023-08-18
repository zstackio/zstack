//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RoutingBridgingRolesType extends ApiPropertyBase {
    List<String> rb_roles;
    public RoutingBridgingRolesType() {
    }
    public RoutingBridgingRolesType(List<String> rb_roles) {
        this.rb_roles = rb_roles;
    }
    
    public List<String> getRbRoles() {
        return rb_roles;
    }
    
    
    public void addRbRoles(String obj) {
        if (rb_roles == null) {
            rb_roles = new ArrayList<String>();
        }
        rb_roles.add(obj);
    }
    public void clearRbRoles() {
        rb_roles = null;
    }
    
}
