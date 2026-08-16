package org.zstack.identity.rbac;

import org.zstack.header.identity.role.api.APICreateRoleMsg;

/**
 * Wraps role creation so extensions can keep their final authorization check
 * and the persisted role mutation in one transaction.
 */
public interface RoleCreationExtensionPoint {
    void beforeRoleCreation(APICreateRoleMsg msg);

    default void createRole(APICreateRoleMsg msg, Runnable creation) {
        beforeRoleCreation(msg);
        creation.run();
    }
}
