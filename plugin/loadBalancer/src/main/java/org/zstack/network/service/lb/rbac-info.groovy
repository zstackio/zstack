package org.zstack.network.service.lb

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "load-balancer"
            normalAPIs("org.zstack.network.service.lb.**")
            targetResources = [LoadBalancerVO.class]
        }

        role {
            name = "load-balancer"
            uuid = "cfc42f6e27be4fcc9e93b09356074e7e"
            normalActionsFromRBAC("load-balancer", "vip")
        }
    }
}