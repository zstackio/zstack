package org.zstack.appliancevm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "appliance-vm"
            targetResources = [ApplianceVmVO.class]
            normalAPIs("org.zstack.appliancevm.**")
        }

        contributeToRole {
            roleName = "vrouter"

            normalActionsFromRBAC("appliance-vm")
        }
    }
}