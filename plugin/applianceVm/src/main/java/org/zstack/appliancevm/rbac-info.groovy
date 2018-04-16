package org.zstack.appliancevm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "appliance-vm"
            normalAPIs("org.zstack.appliancevm.**")
        }

        contributeToRole {
            roleName = "vrouter"

            normalActionsFromRBAC("appliance-vm")
        }
    }
}