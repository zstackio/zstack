package org.zstack.header.identity;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg;
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("identity")
                .adminOnlyAPIs(
                        APICreateAccountMsg.class,
                        APIShareResourceMsg.class,
                        APIRevokeResourceSharingMsg.class,
                        APIUpdateQuotaMsg.class,
                        APIQuerySharedResourceMsg.class,
                        APIChangeResourceOwnerMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class
                ).normalAPIs("org.zstack.header.identity.**")
                .targetResources(AccountVO.class, PolicyVO.class, RoleVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("identity")
                .permissionsByName("identity")
                .uuid("acf2695d8c7c4c5587f5b136098fe45e")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
