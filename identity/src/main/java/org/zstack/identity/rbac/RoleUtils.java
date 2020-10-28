package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.role.RolePolicyStatementVO;
import org.zstack.header.identity.role.RolePolicyStatementVO_;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;

/**
 * Created by Qi Le on 2021/6/3
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RoleUtils {
    @Autowired
    private PluginRegistry pluginRgty;
    public void deleteRole(String uuid) {
        for(DeleteRoleExtensionPoint ext : pluginRgty.getExtensionList(DeleteRoleExtensionPoint.class)) {
            ext.beforeDeleteRole(uuid);
        }

        SQL.New(RoleVO.class).eq(RoleVO_.uuid, uuid).hardDelete();
        SQL.New(RolePolicyStatementVO.class).eq(RolePolicyStatementVO_.roleUuid, uuid).hardDelete();
    }
}
