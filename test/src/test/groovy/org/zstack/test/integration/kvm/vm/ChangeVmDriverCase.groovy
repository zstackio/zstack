package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.identity.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class ChangeVmDriverCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

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
            testChangeVmDriver()
        }
    }

    void testChangeVmDriver() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        setVmDriver {
            uuid = vm.uuid
            virtio = false
        }
        def vmVirtio = VmSystemTags.VIRTIO.hasTag(vm.uuid, VmInstanceVO.class)
        assert !vmVirtio

        rebootVmInstance {
            uuid = vm.uuid
        }
        assert cmd.rootVolume.useVirtio == vmVirtio

        setVmDriver {
            uuid = vm.uuid
            virtio = true
        }
        vmVirtio = VmSystemTags.VIRTIO.hasTag(vm.uuid, VmInstanceVO.class)
        assert vmVirtio

        rebootVmInstance {
            uuid = vm.uuid
        }
        assert cmd.rootVolume.useVirtio == vmVirtio
    }
}
