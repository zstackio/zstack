package org.zstack.header.identity;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg;
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "identity";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs(
                        APICreateAccountMsg.class,
                        APIShareResourceMsg.class,
                        APIRevokeResourceSharingMsg.class,
                        APIUpdateQuotaMsg.class,
                        APIQuerySharedResourceMsg.class,
                        APIChangeResourceOwnerMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class,
                        APICheckResourcePermissionMsg.class
                )
                .targetResources(AccountVO.class, PolicyVO.class, RoleVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("acf2695d8c7c4c5587f5b136098fe45e")
                .permissionBaseOnThis()
                .build();
    }
}
