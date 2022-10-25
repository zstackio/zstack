//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class NodeProfileRolesType extends ApiPropertyBase {
    List<NodeProfileRoleType> role_mappings;
    public NodeProfileRolesType() {
    }
    public NodeProfileRolesType(List<NodeProfileRoleType> role_mappings) {
        this.role_mappings = role_mappings;
    }
    
    public List<NodeProfileRoleType> getRoleMappings() {
        return role_mappings;
    }
    
    
    public void addRoleMappings(NodeProfileRoleType obj) {
        if (role_mappings == null) {
            role_mappings = new ArrayList<NodeProfileRoleType>();
        }
        role_mappings.add(obj);
    }
    public void clearRoleMappings() {
        role_mappings = null;
    }
    
    
    public void addRoleMappings(String physical_role, List<String> rb_roles) {
        if (role_mappings == null) {
            role_mappings = new ArrayList<NodeProfileRoleType>();
        }
        role_mappings.add(new NodeProfileRoleType(physical_role, rb_roles));
    }
    
}
