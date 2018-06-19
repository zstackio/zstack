package org.zstack.header.identity

import org.zstack.header.core.StaticInit
import org.zstack.header.identity.role.RoleVO
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "identity"
            adminOnlyAPIs(
                    APICreateAccountMsg.class.name,
                    APIShareResourceMsg.class.name,
                    APIRevokeResourceSharingMsg.class.name,
                    APIUpdateQuotaMsg.class.name,
                    APIQuerySharedResourceMsg.class.name,
                    APIChangeResourceOwnerMsg.class.name,
                    APIAttachRoleToAccountMsg.class.name,
                    APIDetachRoleFromAccountMsg.class.name
            )

            targetResources = [AccountVO.class, PolicyVO.class, RoleVO.class]

            normalAPIs("org.zstack.header.identity.**")
        }

        role {
            uuid = "acf2695d8c7c4c5587f5b136098fe45e"
            name = "identity"
            normalActionsFromRBAC("identity")
        }
    }
}

