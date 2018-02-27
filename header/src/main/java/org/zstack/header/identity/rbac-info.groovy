package org.zstack.header.identity

import org.zstack.header.core.StaticInit
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg
import org.zstack.header.message.APIMessage

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
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
}

