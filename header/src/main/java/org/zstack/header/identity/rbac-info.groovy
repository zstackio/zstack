package org.zstack.header.identity

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        adminOnlyAPIs("^org.zstack.header.identity.*")

        normalAPIs()
    }
}

