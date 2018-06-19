package org.zstack.header.host

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.host.**")
            normalAPIs(APIQueryHostMsg.class.name)
        }


        contributeToRole {
            roleName = "other"
            actions(APIQueryHostMsg.class.name)
        }
    }
}

