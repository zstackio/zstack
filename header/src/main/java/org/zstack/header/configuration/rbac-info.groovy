package org.zstack.header.configuration

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "configuration"
            adminOnlyAPIs("org.zstack.header.configuration.**")

            normalAPIs(
                    APIQueryDiskOfferingMsg.class.name,
                    APIQueryInstanceOfferingMsg.class.name
            )
        }

        role {
            name = "configuration"
            uuid = "067c4dc358e847aba47903ca4fb1c41c"
            normalActionsFromRBAC("configuration")
        }
    }
}

