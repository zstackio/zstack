package org.zstack.identity.rbac;

import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.RoleMessage;

public interface RoleMutationExtensionPoint {
    void beforeRoleMutation(RoleVO role, RoleMessage msg);
}
