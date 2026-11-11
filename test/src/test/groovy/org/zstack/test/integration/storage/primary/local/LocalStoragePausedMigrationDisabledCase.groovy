package org.zstack.test.integration.storage.primary.local

import org.zstack.sdk.VmInstanceInventory
import org.zstack.header.vm.VmInstanceState
import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig

class LocalStoragePausedMigrationDisabledCase extends LiveMigrateVmCase {
    @Override
    void test() {
        env.create {
            def vm = env.inventoryByName("vm") as VmInstanceInventory
            pauseVmInstance {
                delegate.uuid = vm.uuid
            }
            env.afterSimulator(KVMConstant.KVM_VM_CHECK_STATE) { KVMAgentCommands.CheckVmStateRsp rsp, HttpEntity<String> e ->
                if (rsp.states.containsKey(vm.uuid)) {
                    rsp.states.put(vm.uuid, KVMConstant.KvmVmState.Paused.toString())
                }
                return rsp
            }
            testRejectLiveMigrationWhenDisabled()

            LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION.updateValue("true")
            def target = env.inventoryByName("kvm1")
            def migrated = migrateVm {
                delegate.vmInstanceUuid = vm.uuid
                delegate.hostUuid = target.uuid
            } as VmInstanceInventory
            assert migrated.hostUuid == target.uuid
            assert migrated.state == VmInstanceState.Paused.toString()
        }
    }
}
