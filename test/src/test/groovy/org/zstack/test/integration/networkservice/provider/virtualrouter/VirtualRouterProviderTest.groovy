package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class VirtualRouterProviderTest extends Test {
    static SpringSpec springSpec = makeSpring {
        kvm()
        localStorage()
        sftpBackupStorage()
        eip()
        portForwarding()
        lb()
        virtualRouter()
        vyos()
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
        runSubCases([

        ])
    }
}
