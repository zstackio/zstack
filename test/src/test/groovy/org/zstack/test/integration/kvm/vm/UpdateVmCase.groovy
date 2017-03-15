package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmSystemTags
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.data.SizeUnit


/**
 * Created by Camile on 2017/3/15.
 */
class UpdateVmCase extends SubCase {
    EnvSpec env

    def DOC = """
"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testUpdateMemoryAndCpuForVmSuccess()
        }
    }

    void testUpdateMemoryAndCpuForVmSuccess() {
        VmSpec spec = env.specByName("vm")

        VmInstanceInventory viInv = updateVmInstance {
            uuid = spec.inventory.uuid
            sessionId = adminSession()
            cpuNum = 10
            memorySize = SizeUnit.GIGABYTE.toByte(2)
        }
        assert viInv.uuid == spec.inventory.uuid
        assert viInv.cpuNum == 10
        assert viInv.memorySize == SizeUnit.GIGABYTE.toByte(2)
        assert "10" ==  VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(spec.inventory.uuid, VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN)
        assert SizeUnit.GIGABYTE.toByte(2)+"" ==  VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(spec.inventory.uuid, VmSystemTags.PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN)

        VmInstanceInventory viInv2 = updateVmInstance {
            uuid = spec.inventory.uuid
            sessionId = adminSession()
            cpuNum = 20
            memorySize = SizeUnit.GIGABYTE.toByte(3)
        }
        assert viInv2.uuid == spec.inventory.uuid
        assert viInv2.cpuNum == 20
        assert viInv2.memorySize == SizeUnit.GIGABYTE.toByte(3)
        assert "20" ==  VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(spec.inventory.uuid, VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN)
        assert SizeUnit.GIGABYTE.toByte(3)+"" ==  VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(spec.inventory.uuid, VmSystemTags.PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN)
    }


    @Override
    void clean() {
        env.delete()
    }
}
