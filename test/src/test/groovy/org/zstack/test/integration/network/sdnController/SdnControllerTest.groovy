package org.zstack.test.integration.network.sdnController

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by shixin on 09/26/2019.
 */
class SdnControllerTest extends Test {
    static SpringSpec springSpec = makeSpring {
        virtualRouter()
        vyos()
        kvm()
        localStorage()
        sftpBackupStorage()
        flatNetwork()
        securityGroup()
        nfsPrimaryStorage()
        include("vip.xml")
        include("vxlan.xml")
        include("sdnController.xml")
        include("sugonSdnController.xml")
        include("TfPortAllocator.xml")
        eip()
        lb()
        portForwarding()
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
