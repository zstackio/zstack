package org.zstack.header.host

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        prefix "org.zstack.header.host"

        adminOnlyAPIs("^org.zstack.header.host.*")
    }
}

