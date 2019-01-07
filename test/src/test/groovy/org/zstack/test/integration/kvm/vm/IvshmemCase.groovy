package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.config.GlobalConfigException
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.VmInstanceInventory
import org.zstack.tag.SystemTagCreator
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class IvshmemCase extends SubCase {
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
            testStartVm()
        }
    }

    void testStartVm() {
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.ivshmem == null
            return rsp
        }

        def vm = env.inventoryByName("vm") as VmInstanceInventory
        rebootVmInstance {
            uuid = vm.uuid
        }

        KVMGlobalConfig.IVSHMEM_SIZE.updateValue(SizeUnit.MEGABYTE.toByte(8))

        rebootVmInstance {
            uuid = vm.uuid
        }
        
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.ivshmem != null
            assert cmd.ivshmem.size == KVMGlobalConfig.IVSHMEM_SIZE.value(Long.class)
            assert cmd.ivshmem.namePrefix == KVMGlobalProperty.VM_IVSHMEM_DEV_PREFIX

            return rsp
        }

        SystemTagCreator creator = KVMSystemTags.LIBVIRT_VERSION.newSystemTagCreator(vm.hostUuid)
        creator.setTagByTokens(Collections.singletonMap(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "4.0.0"))
        creator.inherent = true // for test it is inherent
        creator.recreate = true
        creator.create()

        rebootVmInstance {
            uuid = vm.uuid
        }

        KVMGlobalConfig.IVSHMEM_SIZE.updateValue(0)

        expect (GlobalConfigException.class) {
            KVMGlobalConfig.IVSHMEM_SIZE.updateValue(4)
        }

        expect (GlobalConfigException.class) {
            KVMGlobalConfig.IVSHMEM_SIZE.updateValue("asd")
        }

        expect (GlobalConfigException.class) {
            KVMGlobalConfig.IVSHMEM_SIZE.updateValue(SizeUnit.MEGABYTE.toByte(3))
        }
    }
}
