package org.zstack.test.integration.networkservice.provider

import org.zstack.test.integration.networkservice.provider.flat.userdata.OneVmUserdata
import org.zstack.test.integration.networkservice.provider.flat.dhcp.OneVmDhcp
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class NetworkServiceProviderTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        sftpBackupStorage()
        portForwarding()
        virtualRouter()
        flatNetwork()
        eip()
        lb()
        vyos()
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
        runSubCases([
                // flat network provider
                new OneVmDhcp(),
                new OneVmUserdata()
        ])
    }
}
