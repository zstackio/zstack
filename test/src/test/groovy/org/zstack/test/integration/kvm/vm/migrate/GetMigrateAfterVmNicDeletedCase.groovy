package org.zstack.test.integration.kvm.vm.migrate

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.allocator.DesignatedAllocateHostMsg
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2017/12/26.
 */
class GetMigrateAfterVmNicDeletedCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

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
        env = VmMigrateEnv.oneVmThreeHostsNfsStorage()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testGetVmMigrateHosts()
            testMigrate()
            testCurrentFileVolumeConfiguration()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
    }

    void testGetVmMigrateHosts() {
        def hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 2

        vm = detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics.get(0).uuid
        } as VmInstanceInventory

        assert vm.vmNics.size() == 0
        hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 2
    }

    void testMigrate() {
        def host = env.inventoryByName("kvm") as HostInventory

        CloudBus bus = bean(CloudBus.class)
        boolean allowNoL3 = false
        int count = 0
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            void beforeDeliveryMessage(Message msg) {
                DesignatedAllocateHostMsg dmsg = (DesignatedAllocateHostMsg) msg
                count += 1
                allowNoL3 = dmsg.isAllowNoL3Networks()
            }
        }, DesignatedAllocateHostMsg.class)

        vm = migrateVm {
            vmInstanceUuid = vm.uuid
        } as VmInstanceInventory

        assert vm.hostUuid != host.uuid
        assert count == 1
        assert allowNoL3
    }

    void testCurrentFileVolumeConfiguration() {
        HostInventory destination = env.inventoryByName("kvm")
        updateResourceConfig {
            resourceUuid = vm.rootVolumeUuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.LIBVIRT_CACHE_MODE.name
            value = "writethrough"
        }

        KVMAgentCommands.MigrateVmCmd command = null
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> request ->
            command = JSONObjectUtil.toObject(request.body, KVMAgentCommands.MigrateVmCmd.class)
            return rsp
        }
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = destination.uuid
        }

        assert command != null : "Migration must send a command: expected=non-null actual=${command}"
        assert command.notMigrateDisks?.size() == 1 :
                "File volume must participate in configuration refresh: expected=1 actual=${command.notMigrateDisks?.size()}"
        String rootPath = vm.allVolumes.find { it.uuid == vm.rootVolumeUuid }.installPath
        def volume = command.notMigrateDisks[rootPath]
        assert volume.volumeUuid == vm.rootVolumeUuid :
                "Refreshed volume identity must match: expected=${vm.rootVolumeUuid} actual=${volume.volumeUuid}"
        assert volume.cacheMode == "writethrough" :
                "Migration must read current volume cache mode: expected=writethrough actual=${volume.cacheMode}"
        assert !command.disks :
                "Refreshing shared file volume must not copy data: expected=empty actual=${command.disks}"
    }
}
