package org.zstack.test.integration.kvm.host.deletion

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.SQL
import org.zstack.header.host.HostState
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeAttachedJudger
import org.zstack.header.volume.VolumeInventory as InternalVolumeInventory
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.DeleteHostAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class DeleteHostWithNonLocalVmCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "offering"
                cpu = 1
                memory = SizeUnit.MEGABYTE.toByte(512)
            }
            diskOffering {
                name = "disk"
                diskSize = SizeUnit.GIGABYTE.toByte(1)
            }
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                hostname = "localhost"
                username = "root"
                password = "password"
                image {
                    name = "image"
                    architecture = "x86_64"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                    (1..4).each { index ->
                        kvm {
                            name = "host${index}"
                            managementIp = "127.0.0.${index}"
                            username = "root"
                            password = "password"
                        }
                    }
                    attachPrimaryStorage("local")
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }
                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }
                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testNonLocalRootVolumeAndStoppedVm()
            testNonLocalDataVolumeAndLocalOnlyVm()
        }
    }

    VmInstanceInventory createVm(String hostName, String storageName) {
        return createVmInstance {
            name = "${hostName}-${storageName}"
            hostUuid = (env.inventoryByName(hostName) as HostInventory).uuid
            primaryStorageUuidForRootVolume = (env.inventoryByName(storageName) as PrimaryStorageInventory).uuid
            instanceOfferingUuid = env.inventoryByName("offering").uuid
            imageUuid = env.inventoryByName("image").uuid
            l3NetworkUuids = [env.inventoryByName("l3").uuid]
        }
    }

    void assertDeleteRejected(HostInventory host, VmInstanceInventory vm, String storageUuid) {
        VmInstanceVO before = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        def state = before.state
        def currentHost = before.hostUuid
        def lastHost = before.lastHostUuid
        ["Permissive", "Enforcing"].each { mode ->
            def result = new DeleteHostAction(uuid: host.uuid, deleteMode: mode,
                    sessionId: currentEnvSpec.session.uuid).call()
            assert result.error != null
            assert result.error.details.contains("non-local primary storage")
            assert result.error.details.contains(vm.uuid)
            assert result.error.details.contains(vm.name)
            assert result.error.details.contains(storageUuid)
            assert dbFindByUuid(host.uuid, HostVO.class).state == HostState.Enabled
            VmInstanceVO after = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert after.state == state
            assert after.hostUuid == currentHost
            assert after.lastHostUuid == lastHost
        }
    }

    void testNonLocalRootVolumeAndStoppedVm() {
        HostInventory source = env.inventoryByName("host1")
        HostInventory destination = env.inventoryByName("host2")
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        VmInstanceInventory vm = createVm("host1", "nfs")
        assertDeleteRejected(source, vm, nfs.uuid)

        stopVmInstance { uuid = vm.uuid }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == null
        assertDeleteRejected(source, vm, nfs.uuid)

        startVmInstance { uuid = vm.uuid; hostUuid = source.uuid }
        migrateVm { vmInstanceUuid = vm.uuid; hostUuid = destination.uuid }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).lastHostUuid == source.uuid
        deleteHost { uuid = source.uuid }
        assert dbFindByUuid(source.uuid, HostVO.class) == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == destination.uuid
        assertDeleteRejected(destination, vm, nfs.uuid)

        String originalPolicy = VmGlobalConfig.VM_DELETION_POLICY.value()
        try {
            VmGlobalConfig.VM_DELETION_POLICY.updateValue("Delay")
            destroyVmInstance { uuid = vm.uuid }
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
            deleteHost { uuid = destination.uuid }
            assert dbFindByUuid(destination.uuid, HostVO.class) == null
        } finally {
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(originalPolicy)
        }
    }

    void testNonLocalDataVolumeAndLocalOnlyVm() {
        HostInventory host = env.inventoryByName("host3")
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        VmInstanceInventory vm = createVm("host3", "local")
        VolumeInventory data = createDataVolume {
            name = "nfs-data"
            primaryStorageUuid = nfs.uuid
            diskOfferingUuid = env.inventoryByName("disk").uuid
        }
        attachDataVolumeToVm { vmInstanceUuid = vm.uuid; volumeUuid = data.uuid }
        assertDeleteRejected(host, vm, nfs.uuid)
        detachDataVolumeFromVm { uuid = data.uuid }

        // Exercise the shareable-volume attachment extension without requiring
        // the premium shareable-volume implementation in the core KVM suite.
        List<VolumeAttachedJudger> original = bean(PluginRegistry.class).getExtensionList(VolumeAttachedJudger.class)
        VolumeAttachedJudger shared = [
                isAttached: { InternalVolumeInventory volume -> volume.uuid == data.uuid },
                getAttachedVmUuids: { InternalVolumeInventory volume -> volume.uuid == data.uuid ? [vm.uuid] : [] }
        ] as VolumeAttachedJudger
        try {
            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, data.uuid).set(VolumeVO_.isShareable, true).update()
            InternalVolumeInventory.setAttachedJudgers(original + [shared])
            assert dbFindByUuid(data.uuid, VolumeVO.class).vmInstanceUuid == null
            assertDeleteRejected(host, vm, nfs.uuid)
        } finally {
            InternalVolumeInventory.setAttachedJudgers(original)
            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, data.uuid).set(VolumeVO_.isShareable, false).update()
        }

        // A detached non-local volume and a local-only VM do not block deletion.
        deleteHost { uuid = host.uuid }
        assert dbFindByUuid(host.uuid, HostVO.class) == null
        deleteHost { uuid = (env.inventoryByName("host4") as HostInventory).uuid }
    }

    @Override
    void clean() {
        env.delete()
    }
}
