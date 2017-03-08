package org.zstack.test.integration.kvm.hostallocator

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by david on 3/6/17.
 */
class AllocatorTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        sftpBackupStorage()
        kvm()
        securityGroup()
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
