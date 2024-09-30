package org.zstack.header.identity;

import org.zstack.header.identity.login.APIGetLoginProceduresMsg;
import org.zstack.header.identity.login.APILogInMsg;
import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.APIDeleteRoleMsg;
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg;
import org.zstack.header.identity.role.api.APIQueryRoleMsg;
import org.zstack.header.identity.role.api.APIUpdateRoleMsg;

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
                        APIChangeResourceOwnerMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class,
                        APICheckResourcePermissionMsg.class
                )
                .targetResources(AccountVO.class, RoleVO.class)
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
                .excludeActions(
                        APICreateRoleMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class,
                        APIDeleteRoleMsg.class,
                        APIUpdateRoleMsg.class
                )
                .build();

        roleBuilder()
                .uuid("09380f1d01183826b97e36ad04083677")
                .name("role")
                .actions(
                        APICreateRoleMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class,
                        APIDeleteRoleMsg.class,
                        APIUpdateRoleMsg.class,
                        APIQueryRoleMsg.class
                )
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .actions(
                        APILogInMsg.class,
                        APILogOutMsg.class,
                        APIGetLoginProceduresMsg.class,
                        APIQueryRoleMsg.class,
                        APIGetAccountQuotaUsageMsg.class,
                        APIGetResourceAccountMsg.class,
                        APILogInByAccountMsg.class,
                        APIQueryAccountMsg.class,
                        APIQueryAccountResourceRefMsg.class,
                        APIQueryQuotaMsg.class,
                        APIRenewSessionMsg.class,
                        APIRevokeResourceSharingMsg.class,
                        APIShareResourceMsg.class,
                        APIUpdateAccountMsg.class,
                        APIValidateSessionMsg.class
                )
                .toOtherRole()
                .build();
    }
}
