//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class NodeProfileRoleType extends ApiPropertyBase {
    String physical_role;
    List<String> rb_roles;
    public NodeProfileRoleType() {
    }
    public NodeProfileRoleType(String physical_role, List<String> rb_roles) {
        this.physical_role = physical_role;
        this.rb_roles = rb_roles;
    }
    public NodeProfileRoleType(String physical_role) {
        this(physical_role, null);    }
    
    public String getPhysicalRole() {
        return physical_role;
    }
    
    public void setPhysicalRole(String physical_role) {
        this.physical_role = physical_role;
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
