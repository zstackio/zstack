package org.zstack.header.core.webhooks

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.core.webhooks.**")
        }
    }
}