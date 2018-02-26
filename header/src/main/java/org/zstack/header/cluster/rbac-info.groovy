package org.zstack.header.cluster

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        prefix "org.zstack.header.cluster"

        adminOnlyAPIs("^org.zstack.header.cluster.*")
    }
}

