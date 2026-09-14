package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO
import org.zstack.header.vm.VmMachineType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupVO
import org.zstack.header.vm.devices.VmInstanceDeviceAddressVO
import org.zstack.header.vm.devices.VmInstanceDeviceAddressVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10343
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10344
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10345

class VmMachineTypeCase extends SubCase {
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
            testSetVmMachineTypeWithMemorySnapshot()
            testSetVmMachineType()
        }
    }

    void testSetVmMachineTypeWithMemorySnapshot() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def dbf = bean(DatabaseFacade.class)

        stopVmInstance {
            uuid = vm.uuid
        }

        def snapshotGroup = new VolumeSnapshotGroupVO()
        snapshotGroup.uuid = Platform.uuid
        snapshotGroup.name = "memory-snapshot-group"
        snapshotGroup.snapshotCount = 1
        snapshotGroup.vmInstanceUuid = vm.uuid
        snapshotGroup.accountUuid = env.session.accountUuid
        dbf.persistAndRefresh(snapshotGroup)

        def addressGroup = new VmInstanceDeviceAddressGroupVO()
        addressGroup.uuid = Platform.uuid
        addressGroup.resourceUuid = snapshotGroup.uuid
        addressGroup.vmInstanceUuid = vm.uuid
        dbf.persistAndRefresh(addressGroup)

        expectApiFailure({
            setVmMachineType {
                uuid = vm.uuid
                machineType = VmMachineType.q35.toString()
            }
        }) {
            assert globalErrorCode == ORG_ZSTACK_COMPUTE_VM_10345
        }

        dbf.remove(addressGroup)
        dbf.remove(snapshotGroup)
    }

    void testSetVmMachineType() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def dbf = bean(DatabaseFacade.class)

        stopVmInstance {
            uuid = vm.uuid
        }

        assert !VmSystemTags.MACHINE_TYPE.hasTag(vm.uuid)
        assert Q.New(VmInstanceDeviceAddressVO.class)
                .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vm.uuid)
                .count() != 0

        setVmMachineType {
            uuid = vm.uuid
            machineType = VmMachineType.q35.toString()
        }

        assert VmMachineType.q35.toString() == VmSystemTags.MACHINE_TYPE.getTokenByResourceUuid(
                vm.uuid, VmSystemTags.MACHINE_TYPE_TOKEN)
        assert Q.New(VmInstanceDeviceAddressVO.class)
                .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vm.uuid)
                .count() == 0

        def snapshotGroup = createMemorySnapshotGroup(vm.uuid)
        setVmMachineType {
            uuid = vm.uuid
            machineType = VmMachineType.q35.toString()
        }
        assert VmMachineType.q35.toString() == VmSystemTags.MACHINE_TYPE.getTokenByResourceUuid(
                vm.uuid, VmSystemTags.MACHINE_TYPE_TOKEN)
        dbf.removeByPrimaryKey(snapshotGroup.uuid, VmInstanceDeviceAddressGroupVO.class)
        dbf.remove(snapshotGroup)

        VmInstanceInventory runningVm = startVmInstance {
            uuid = vm.uuid
        }
        assert runningVm.state == VmInstanceState.Running.toString()

        expectApiFailure({
            setVmMachineType {
                uuid = vm.uuid
                machineType = VmMachineType.q35.toString()
            }
        }) {
            assert globalErrorCode == ORG_ZSTACK_COMPUTE_VM_10343
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        VmSystemTags.MACHINE_TYPE.delete(vm.uuid)
        def virtMachineTypeTag = VmSystemTags.MACHINE_TYPE.newSystemTagCreator(vm.uuid)
        virtMachineTypeTag.setTagByTokens([(VmSystemTags.MACHINE_TYPE_TOKEN): VmMachineType.virt.toString()])
        virtMachineTypeTag.create()

        expectApiFailure({
            setVmMachineType {
                uuid = vm.uuid
                machineType = VmMachineType.q35.toString()
            }
        }) {
            assert globalErrorCode == ORG_ZSTACK_COMPUTE_VM_10344
        }

        VmSystemTags.MACHINE_TYPE.delete(vm.uuid)
        def q35MachineTypeTag = VmSystemTags.MACHINE_TYPE.newSystemTagCreator(vm.uuid)
        q35MachineTypeTag.setTagByTokens([(VmSystemTags.MACHINE_TYPE_TOKEN): VmMachineType.q35.toString()])
        q35MachineTypeTag.create()

        setVmBootMode {
            uuid = vm.uuid
            bootMode = "UEFI"
        }

        setVmMachineType {
            uuid = vm.uuid
            machineType = VmMachineType.q35.toString()
        }

        assert VmMachineType.q35.toString() == VmSystemTags.MACHINE_TYPE.getTokenByResourceUuid(
                vm.uuid, VmSystemTags.MACHINE_TYPE_TOKEN)
    }

    private VolumeSnapshotGroupVO createMemorySnapshotGroup(String vmUuid) {
        def dbf = bean(DatabaseFacade.class)
        def snapshotGroup = new VolumeSnapshotGroupVO()
        snapshotGroup.uuid = Platform.uuid
        snapshotGroup.name = "memory-snapshot-group"
        snapshotGroup.snapshotCount = 1
        snapshotGroup.vmInstanceUuid = vmUuid
        snapshotGroup.accountUuid = env.session.accountUuid
        dbf.persistAndRefresh(snapshotGroup)

        def addressGroup = new VmInstanceDeviceAddressGroupVO()
        addressGroup.uuid = snapshotGroup.uuid
        addressGroup.resourceUuid = snapshotGroup.uuid
        addressGroup.vmInstanceUuid = vmUuid
        dbf.persistAndRefresh(addressGroup)
        return snapshotGroup
    }
}
