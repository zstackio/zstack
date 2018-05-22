package org.zstack.header.network.service

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.network.service.**")
        }

        contributeToRole {
            roleName = "networks"
            actions(APIQueryNetworkServiceProviderMsg.class.name)
        }
    }
}