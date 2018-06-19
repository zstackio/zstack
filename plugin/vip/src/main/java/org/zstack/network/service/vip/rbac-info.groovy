package org.zstack.network.service.vip

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "vip"
            normalAPIs("org.zstack.network.service.vip.**")
            targetResources = [VipVO.class]
        }
    }
}