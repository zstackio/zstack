package org.zstack.identity.rbac;

public interface DeleteRoleExtensionPoint {
    void beforeDeleteRole(String roleUuid);
}
