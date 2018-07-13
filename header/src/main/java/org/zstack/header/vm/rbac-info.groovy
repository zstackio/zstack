package org.zstack.header.vm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "vm"
            normalAPIs("org.zstack.header.vm.**")

            targetResources = [VmInstanceVO.class]
        }

        role {
            uuid = "5f93cf6444ec44cc83209744c8c3d7cc"
            name = "vm"
            normalActionsFromRBAC("vm")
        }

        role {
            uuid = "d6b79564f9b641a4b8bb85ea249151c2"
            name = "vm-operation-without-create-permission"
            normalActionsFromRBAC("vm")
            excludedActions = [APICreateVmInstanceMsg.class.name]
        }
    }
}

