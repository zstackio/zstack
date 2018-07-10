package org.zstack.kvm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.kvm.**")
        }
    }
}