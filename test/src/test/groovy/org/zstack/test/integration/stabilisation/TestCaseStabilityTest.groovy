package org.zstack.test.integration.stabilisation

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/7/15.
 *
 * Example:
 * mvn test -Dtest=TestCaseStabilityTest -Dcases=org.zstack.test.integration.core.MustPassCase -Dtimes=3
 *
 * Q : How do I know which subcase failed ?
 * A : search keyword is 'stability test fails', likes: grep "stability test fails" management-server.log
 */
class TestCaseStabilityTest extends Test {
    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        nfsPrimaryStorage()
        virtualRouter()
        flatNetwork()
        securityGroup()
        kvm()
        ceph()
        smp()
        vyos()
        portForwarding()
        eip()
        lb()
        include("vip.xml")
        include("vxlan.xml")
        include("LdapManagerImpl.xml")
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
