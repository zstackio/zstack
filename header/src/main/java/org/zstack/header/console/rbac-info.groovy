package org.zstack.header.console

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "console"
            adminOnlyAPIs("org.zstack.header.console.**")

            normalAPIs(
                    APIRequestConsoleAccessMsg.class.name,
            )
        }

        role {
            name = "console"
            uuid = "6f5a7d6d2da9499da9e4bdb079f65adf"
            normalActionsFromRBAC("console")
        }
    }
}

