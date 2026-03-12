package org.zstack.test.integration.kvm.vm

import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * 最小化虚拟机测试用例
 * 仅验证虚拟机创建后的状态
 */
class SimpleVmLifeCycleCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmTwoHostNfsEnv()
    }

    @Override
    void test() {
        env.create {
            testVmIsRunningAfterCreation()
        }
    }

    /**
     * 测试：创建后虚拟机应处于 Running 状态
     */
    void testVmIsRunningAfterCreation() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo != null
        assert vo.state == VmInstanceState.Running
    }

    @Override
    void clean() {
        env.delete()
    }
}
