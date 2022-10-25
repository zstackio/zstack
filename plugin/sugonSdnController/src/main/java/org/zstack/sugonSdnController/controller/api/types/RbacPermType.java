//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RbacPermType extends ApiPropertyBase {
    String role_name;
    String role_crud;
    public RbacPermType() {
    }
    public RbacPermType(String role_name, String role_crud) {
        this.role_name = role_name;
        this.role_crud = role_crud;
    }
    public RbacPermType(String role_name) {
        this(role_name, null);    }
    
    public String getRoleName() {
        return role_name;
    }
    
    public void setRoleName(String role_name) {
        this.role_name = role_name;
    }
    
    
    public String getRoleCrud() {
        return role_crud;
    }
    
    public void setRoleCrud(String role_crud) {
        this.role_crud = role_crud;
    }
    
}
