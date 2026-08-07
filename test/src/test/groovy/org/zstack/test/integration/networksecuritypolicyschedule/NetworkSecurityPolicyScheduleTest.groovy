package org.zstack.test.integration.networksecuritypolicyschedule

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class NetworkSecurityPolicyScheduleTest extends Test {
    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        securityGroup()
        networkSecurityPolicySchedule()
        kvm()
        flatNetwork()
        nfsPrimaryStorage()
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
