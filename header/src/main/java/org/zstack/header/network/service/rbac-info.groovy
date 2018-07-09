package org.zstack.header.network.service

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.network.service.**")
            normalAPIs(
                    APIAttachNetworkServiceToL3NetworkMsg.class.name,
                    APIQueryNetworkServiceProviderMsg.class.name
            )
        }

        contributeToRole {
            roleName = "networks"
            actions(APIQueryNetworkServiceProviderMsg.class.name, APIAttachNetworkServiceToL3NetworkMsg.class.name)
        }
    }
}