package org.zstack.test.integration.kvm

import org.zstack.test.integration.kvm.capacity.CheckHostCapacityWhenAddHostCase
import org.zstack.test.integration.kvm.lifecycle.OneVmBasicLifeCycleCase
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/22.
 */
class KvmTest extends Test {
    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases([
                new OneVmBasicLifeCycleCase(),
                new CheckHostCapacityWhenAddHostCase()
        ])
    }
}
