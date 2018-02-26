package org.zstack.header.configuration

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        adminOnlyAPIs("^org.zstack.header.configuration.*")

        normalAPIs(
                APIQueryDiskOfferingMsg.class.name,
                APIQueryInstanceOfferingMsg.class.name
        )
    }
}

