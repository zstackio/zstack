package org.zstack.header.identity

import org.zstack.header.core.StaticInit
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg

import static org.zstack.header.identity.rbac.RBACInfo.rbac
import static org.zstack.header.identity.rbac.RoleInfo.role

@StaticInit
static void init() {
    def info = rbac {
        adminOnlyAPIs(
                APICreateAccountMsg.class.name,
                APIUpdateAccountMsg.class.name,
                APIShareResourceMsg.class.name,
                APIRevokeResourceSharingMsg.class.name,
                APIUpdateQuotaMsg.class.name,
                APIQuerySharedResourceMsg.class.name,
                APIChangeResourceOwnerMsg.class.name,
                APIAttachRoleToAccountMsg.class.name,
                APIDetachRoleFromAccountMsg.class.name
        )

        normalAPIs("org.zstack.header.identity.**")

    }

    role {
        normalRole {
            uuid = "acf2695d8c7c4c5587f5b136098fe45e"
            name = "identity role"
            allowedActions = info.normalAPIs
        }
    }
}

