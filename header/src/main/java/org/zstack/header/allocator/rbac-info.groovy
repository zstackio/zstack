package org.zstack.header.allocator

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        prefix "org.zstack.header.allocator"

        adminOnlyAPIs("^org.zstack.header.allocator.*")
    }
}

