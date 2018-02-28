package org.zstack.test.integration.identity

import org.junit.Test
import org.zstack.core.Platform
import org.zstack.header.identity.rbac.RBACInfo

class AntMatcherCase {
    @Test
    void test() {
        Platform.getUuid()
        RBACInfo.checkIfAPIsMissingRBACInfo()
    }
}
