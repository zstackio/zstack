package org.zstack.header.longjob

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "long-job"
            normalAPIs("org.zstack.header.longjob.**")
        }

        contributeToRole {
            roleName = "other"
            normalActionsFromRBAC("long-job")
        }
    }
}