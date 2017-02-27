package org.zstack.test.integration.network

import org.zstack.test.integration.network.l3network.getfreeip.OneL3OneIpRangeNoIpUsed
import org.zstack.test.integration.network.l3network.getfreeip.OneL3OneIpRangeSomeIpUsed
import org.zstack.test.integration.network.l3network.getfreeip.OneL3TwoIpRanges
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class NetworkTest extends Test {
    static SpringSpec springSpec = makeSpring {
        virtualRouter()
        vyos()
        kvm()
        localStorage()
        sftpBackupStorage()
        include("vip.xml")
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
                // get free ip
                new OneL3OneIpRangeNoIpUsed(),
                new OneL3OneIpRangeSomeIpUsed(),
                new OneL3TwoIpRanges()
        ])
    }
}
