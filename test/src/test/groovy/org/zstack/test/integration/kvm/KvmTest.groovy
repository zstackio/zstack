package org.zstack.test.integration.kvm

import org.zstack.test.integration.kvm.capacity.CheckHostCapacityWhenAddHostCase
import org.zstack.test.integration.kvm.host.HostStateCase
import org.zstack.test.integration.kvm.vm.OneVmBasicLifeCycleCase
import org.zstack.test.integration.kvm.vm.VmGCCase
import org.zstack.testlib.SpringSpec
import org.zstack.test.integration.kvm.status.MaintainHostCase
import org.zstack.test.integration.kvm.globalconfig.KvmGlobalConfigCase
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/22.
 */
class KvmTest extends Test {
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
        runSubCases([
                new OneVmBasicLifeCycleCase(),
                new CheckHostCapacityWhenAddHostCase(),
                new VmGCCase(),
                new HostStateCase(),
                new MaintainHostCase(),
                new KvmGlobalConfigCase()
        ])
    }
}
