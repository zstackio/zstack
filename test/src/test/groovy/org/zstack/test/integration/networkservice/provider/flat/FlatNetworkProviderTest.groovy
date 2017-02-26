package org.zstack.test.integration.networkservice.provider.flat

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/26.
 */
class FlatNetworkProviderTest extends Test {
    static SpringSpec springSpec = makeSpring {
        flatNetwork()
        kvm()
        localStorage()
        sftpBackupStorage()
        eip()
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
                new OneVmDhcp()
        ])
    }
}
