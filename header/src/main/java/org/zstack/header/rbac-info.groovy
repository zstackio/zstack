package org.zstack.header

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            normalAPIs(APIIsOpensourceVersionMsg.class.name)
        }

        role {
            name = "other"
            uuid = "80315b1f85314917826b182bf6def552"
            actions(APIIsOpensourceVersionMsg.class.name)
        }
    }
}