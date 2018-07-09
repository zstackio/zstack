package org.zstack.network.securitygroup

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "security-group"
            normalAPIs("org.zstack.network.securitygroup.**")
            targetResources = [SecurityGroupVO.class]
        }

        role {
            name = "security-group"
            uuid = "4266a67e46cb4e68864899458187941e"
            normalActionsFromRBAC("security-group")
        }
    }
}