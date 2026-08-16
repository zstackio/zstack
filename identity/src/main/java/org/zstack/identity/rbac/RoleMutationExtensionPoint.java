package org.zstack.identity.rbac;

import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.RoleMessage;

public interface RoleMutationExtensionPoint {
    void beforeRoleMutation(RoleVO role, RoleMessage msg);

    /**
     * Wraps the actual role write so an extension can keep validation locks and
     * the mutation in one transaction. Implementations that only validate keep
     * the historical behavior through this default method.
     */
    default void mutateRole(RoleVO role, RoleMessage msg, Runnable mutation) {
        beforeRoleMutation(role, msg);
        mutation.run();
    }
}
