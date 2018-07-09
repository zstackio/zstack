package org.zstack.header.vo

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            normalAPIs(APIGetResourceNamesMsg.class.name)
        }

        contributeToRole {
            roleName = "other"
            actions(APIGetResourceNamesMsg.class.name)
        }
    }
}