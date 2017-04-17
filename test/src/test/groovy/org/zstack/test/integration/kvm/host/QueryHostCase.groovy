package org.zstack.test.integration.kvm.host

import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class QueryHostCase extends SubCase{

    EnvSpec env

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
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testQueryHostContainsCpuSockets()
        }
    }

    void testQueryHostContainsCpuSockets() {
        def inv = env.inventoryByName("kvm") as HostInventory

        def invs = queryHost {
            conditions=["uuid=${inv.uuid}"]
        } as List<HostInventory>

        assert invs != null
        assert !invs.isEmpty()
        assert invs[0].cpuSockets != null
    }

    @Override
    void clean() {
        env.delete()
    }

}
