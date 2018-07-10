package org.zstack.header.zone

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "zone"
            adminOnlyAPIs("org.zstack.header.zone.**")

            normalAPIs(APIQueryZoneMsg.class.name)
        }

        contributeToRole {
            roleName = "other"
            normalActionsFromRBAC("zone")
        }
    }
}

