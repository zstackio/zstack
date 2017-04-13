package org.zstack.test.integration.identity

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by AlanJager on 2017/3/20.
 */
class IdentityTest extends Test {
    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        virtualRouter()
        securityGroup()
        kvm()
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
