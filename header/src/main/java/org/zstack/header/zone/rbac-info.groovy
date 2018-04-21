package org.zstack.header.zone

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.zone.**")

            normalAPIs(APIQueryZoneMsg.class.name)
        }
    }
}

