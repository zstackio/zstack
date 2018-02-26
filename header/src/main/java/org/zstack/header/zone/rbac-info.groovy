package org.zstack.header.zone

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        prefix "org.zstack.header.zone"

        adminOnlyAPIs("^org.zstack.header.zone.*")
    }
}

