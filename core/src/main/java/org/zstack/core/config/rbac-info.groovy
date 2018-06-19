package org.zstack.core.config

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "global-config"
            adminOnlyAPIs("org.zstack.core.config.**")
            normalAPIs(APIQueryGlobalConfigMsg.class)


            contributeToRole {
                roleName = "other"
                actions(APIQueryGlobalConfigMsg.class.name)
            }
        }
    }
}

