package org.zstack.network.service.portforwarding

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "port-forwarding"
            normalAPIs("org.zstack.network.service.portforwarding.**")
            targetResources = [PortForwardingRuleVO.class]
        }

        role {
            name = "port-forwarding"
            uuid = "62617332af7241dbadf8e0570197d42f"
            normalActionsFromRBAC("port-forwarding", "vip")
        }
    }
}