package org.zstack.core.captcha;

import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.identity.role.RoleVO;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "identity-captcha-refresh";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(AccountVO.class, PolicyVO.class, RoleVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("identity")
                .actionsByPermissionName("identity-captcha-refresh")
                .build();
    }
}
