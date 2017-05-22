package org.zstack.test.integration.image

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/3/3.
 */
class ImageTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        nfsPrimaryStorage()
        sftpBackupStorage()
        smp()
        ceph()
        virtualRouter()
        vyos()
        kvm()
        flatNetwork()
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
