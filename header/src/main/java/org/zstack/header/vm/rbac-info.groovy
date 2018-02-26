package org.zstack.header.vm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        prefix "org.zstack.header.vm"

        normalAPIs("^org.zstack.header.vm.*")
    }
}

