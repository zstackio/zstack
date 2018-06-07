package org.zstack.header.core.progress

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            normalAPIs(
                    APIGetTaskProgressMsg.class.name,
            )
        }

        contributeToRole {
            roleName = "other"
            actions(APIGetTaskProgressMsg.class.name)
        }
    }
}

