package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.rbac.PolicyCreationExtensionPoint;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CheckIfSessionCanOperationAdminPermission {
    @Autowired
    private PluginRegistry pluginRgty;

    /**
     * Get if the session can access admin only api
     * @param session
     * @return the result for access check, if it is true,
     * means can access admin api
     */
    public boolean check(SessionInventory session) {
        boolean access = AccountConstant.isAdmin(session);

        return access || extensionCheck(session);
    }

    private boolean extensionCheck(SessionInventory session) {
        for (PolicyCreationExtensionPoint ext : pluginRgty.getExtensionList(PolicyCreationExtensionPoint.class)) {
            if (!ext.checkPermission(session)) {
                continue;
            }

            return true;
        }

        return false;
    }
}
