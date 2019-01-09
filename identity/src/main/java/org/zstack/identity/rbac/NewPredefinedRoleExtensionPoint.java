package org.zstack.identity.rbac;

import org.zstack.header.identity.role.RoleVO;

public interface NewPredefinedRoleExtensionPoint {
    void predefinedNewRole(RoleVO role);
}
