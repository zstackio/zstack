package org.zstack.test.integration.kvm.vm

import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.springframework.http.HttpEntity

/**
 * 简单的虚拟机生命周期测试用例
 *
 * 测试场景：
 * 1. 创建虚拟机后验证状态为 Running
 * 2. 停止虚拟机后验证状态为 Stopped
 * 3. 启动虚拟机后验证状态恢复为 Running
 * 4. 模拟 KVM stop 命令失败，验证错误处理
 */
class SimpleVmLifeCycleCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        // 使用预置的单虚拟机双主机 NFS 环境
        env = Env.oneVmTwoHostNfsEnv()
    }

    @Override
    void test() {
        env.create {
            testVmIsRunningAfterCreation()
            testStopAndStartVm()
            testStopVmFailWithRollback()
        }
    }

    /**
     * 测试1：创建后虚拟机应处于 Running 状态
     */
    void testVmIsRunningAfterCreation() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo != null
        assert vo.state == VmInstanceState.Running
    }

    /**
     * 测试2：停止虚拟机后状态变为 Stopped，再启动后恢复 Running
     */
    void testStopAndStartVm() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        // 停止虚拟机
        stopVmInstance {
            uuid = vm.uuid
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        // 启动虚拟机
        startVmInstance {
            uuid = vm.uuid
        }

        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    /**
     * 测试3：模拟 KVM agent 停止命令失败，验证虚拟机状态仍为 Running（操作回滚）
     */
    void testStopVmFailWithRollback() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        // 拦截 KVM stop 命令，让其返回失败
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { KVMAgentCommands.StopVmResponse rsp, HttpEntity<String> e ->
            rsp.success = false
            rsp.error = "stop vm failed on purpose"
            return rsp
        }

        // 期望停止操作抛出 AssertionError（API 返回错误）
        expect(AssertionError.class) {
            stopVmInstance {
                uuid = vm.uuid
            }
        }

        // 验证虚拟机状态仍为 Running（操作被正确回滚）
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        // 恢复模拟器，为后续测试清理
        env.cleanAfterSimulatorHandlers()
    }

    @Override
    void clean() {
        env.delete()
    }
}

