package org.zstack.identity.rbac;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.role.RoleIdentity;
import org.zstack.header.identity.role.RoleVO;

public interface RoleIdentityFactory {
    RoleIdentity getIdentity();

    RoleVO createRole(RoleVO vo, SessionInventory session);
}
