package org.zstack.query

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            normalAPIs(APIBatchQueryMsg.class.name)
        }

        contributeToRole {
            roleName = "other"
            actions(APIBatchQueryMsg.class.name)
        }
    }
}