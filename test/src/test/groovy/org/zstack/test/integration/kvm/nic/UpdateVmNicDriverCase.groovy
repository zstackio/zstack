package org.zstack.test.integration.kvm.nic

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.image.ImagePlatform
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.UpdateVmNicDriverAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class UpdateVmNicDriverCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    String nicUuid
    String secondNicUuid
    KVMAgentCommands.StartVmCmd startCmd

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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            nicUuid = vm.vmNics[0].uuid
            env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
                if (cmd.vmInstanceUuid == vm.uuid) {
                    startCmd = cmd
                }
                return rsp
            }

            testChangePlatformWithVirtioEnabled()
            addSecondNic()
            [
                    [ImagePlatform.Other, "FreeBSD"],
                    [ImagePlatform.Other, "Other"],
                    [ImagePlatform.Linux, "Linux"],
                    [ImagePlatform.Windows, "Windows"]
            ].each { os ->
                testUpdateNicDriver(os[0] as ImagePlatform, os[1] as String)
            }
        }
    }

    void testChangePlatformWithVirtioEnabled() {
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert driverOf(nicUuid) == "virtio"

        updateVmInstance {
            uuid = vm.uuid
            platform = ImagePlatform.Other.toString()
            guestOsType = "FreeBSD"
        }
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert driverOf(nicUuid) == "virtio"

        startCmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }
        assertStartCommand([(nicUuid): "virtio"], true)

        stopVmInstance {
            uuid = vm.uuid
        }
        startAndCheck([(nicUuid): "virtio"], true)
    }

    void addSecondNic() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3") as L3NetworkInventory
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = l3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm.uuid
            vmNicUuid = nic.uuid
        }
        secondNicUuid = nic.uuid
        assert driverOf(secondNicUuid) == "virtio"
    }

    void testUpdateNicDriver(ImagePlatform targetPlatform, String targetGuestOs) {
        updateVmInstance {
            uuid = vm.uuid
            platform = targetPlatform.toString()
            guestOsType = targetGuestOs
        }
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)

        // The global switch still controls all NICs on every platform.
        deleteTag {
            uuid = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceUuid, vm.uuid)
                    .eq(SystemTagVO_.tag, VmSystemTags.VIRTIO.tagFormat)
                    .select(SystemTagVO_.uuid).findValue() as String
        }
        assert driverOf(nicUuid) == "e1000"
        assert driverOf(secondNicUuid) == "e1000"

        // A per-NIC override must not enable the global switch or change another NIC.
        VmNicInventory updated = updateVmNicDriver {
            vmInstanceUuid = vm.uuid
            vmNicUuid = nicUuid
            driverType = "virtio"
        }
        assert updated.uuid == nicUuid
        assert updated.driverType == "virtio"
        assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert driverOf(nicUuid) == "virtio"
        assert driverOf(secondNicUuid) == "e1000"

        def action = new UpdateVmNicDriverAction()
        action.sessionId = env.session.uuid
        action.vmInstanceUuid = vm.uuid
        action.vmNicUuid = nicUuid
        action.driverType = "unsupported-driver"
        def result = action.call()
        assert result.error != null
        assert result.error.globalErrorCode == "ORG_ZSTACK_COMPUTE_VM_10115"
        assert driverOf(nicUuid) == "virtio"
        startAndCheck([(nicUuid): "virtio", (secondNicUuid): "e1000"], false)

        createSystemTag {
            resourceType = "VmInstanceVO"
            resourceUuid = vm.uuid
            tag = VmSystemTags.VIRTIO.tagFormat
        }
        assert driverOf(nicUuid) == "virtio"
        assert driverOf(secondNicUuid) == "virtio"

        updateVmNicDriver {
            vmInstanceUuid = vm.uuid
            vmNicUuid = nicUuid
            driverType = "e1000"
        }
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert driverOf(nicUuid) == "e1000"
        assert driverOf(secondNicUuid) == "virtio"
        startAndCheck([(nicUuid): "e1000", (secondNicUuid): "virtio"], true)
    }

    String driverOf(String uuid) {
        return Q.New(VmNicVO.class).eq(VmNicVO_.uuid, uuid)
                .select(VmNicVO_.driverType).findValue() as String
    }

    void startAndCheck(Map<String, String> drivers, boolean virtioEnabled) {
        startCmd = null
        startVmInstance {
            uuid = vm.uuid
        }
        assertStartCommand(drivers, virtioEnabled)
        stopVmInstance {
            uuid = vm.uuid
        }
    }

    void assertStartCommand(Map<String, String> drivers, boolean virtioEnabled) {
        assert startCmd != null
        assert startCmd.nics.size() == drivers.size()
        drivers.each { uuid, driver ->
            def nic = startCmd.nics.find { it.resourceUuid == uuid }
            assert nic != null
            assert nic.driverType == driver
        }
        assert startCmd.rootVolume.useVirtio == virtioEnabled
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid) == virtioEnabled
    }

    @Override
    void clean() {
        env.delete()
    }
}
