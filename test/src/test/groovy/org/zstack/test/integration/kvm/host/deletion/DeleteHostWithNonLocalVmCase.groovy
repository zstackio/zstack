package org.zstack.test.integration.kvm.host.deletion

import org.springframework.test.util.ReflectionTestUtils
import org.zstack.compute.host.HostDeletionGuard
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.SQL
import org.zstack.header.host.HostState
import org.zstack.header.host.HostVO
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.header.host.APIDeleteHostMsg
import org.zstack.header.vm.BuildVolumeSpecExtensionPoint
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeInventory as InternalVolumeInventory
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.DeleteHostAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

        // Resolve additional volumes by the target VM, as the premium shared
        // volume provider does, without a cloud-wide VolumeAttachedJudger scan.
        PluginRegistry registry = bean(PluginRegistry.class)
        Map extensions = ReflectionTestUtils.getField(registry, "extensionsByInterfaceClass") as Map
        List original = extensions.get(BuildVolumeSpecExtensionPoint.class)
        BuildVolumeSpecExtensionPoint shared = { String vmUuid ->
            vmUuid == vm.uuid ? [InternalVolumeInventory.valueOf(dbFindByUuid(data.uuid, VolumeVO.class))] : []
        } as BuildVolumeSpecExtensionPoint
        try {
            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, data.uuid).set(VolumeVO_.isShareable, true).update()
            extensions.put(BuildVolumeSpecExtensionPoint.class, (original ?: []) + [shared])
            assert dbFindByUuid(data.uuid, VolumeVO.class).vmInstanceUuid == null
            assertDeleteRejected(host, vm, nfs.uuid)
        } finally {
            if (original == null) {
                extensions.remove(BuildVolumeSpecExtensionPoint.class)
            } else {
                extensions.put(BuildVolumeSpecExtensionPoint.class, original)
            }
            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, data.uuid).set(VolumeVO_.isShareable, false).update()
        }

        testAttachAfterApiPreflight(host, vm, data, nfs.uuid)
        testDeletionAdmission(host, vm, data)

        // A detached non-local volume and a local-only VM do not block deletion.
        deleteHost { uuid = host.uuid }
        assert dbFindByUuid(host.uuid, HostVO.class) == null
        deleteHost { uuid = (env.inventoryByName("host4") as HostInventory).uuid }
    }

    void testAttachAfterApiPreflight(HostInventory host, VmInstanceInventory vm, VolumeInventory volume, String storageUuid) {
        ["Permissive", "Enforcing"].each { mode ->
            CountDownLatch preflightPassed = new CountDownLatch(1)
            CountDownLatch resumeDelete = new CountDownLatch(1)
            boolean armed = true
            bean(CloudBus.class).installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
                @Override
                void beforeDeliveryMessage(Message message) {
                    APIDeleteHostMsg msg = (APIDeleteHostMsg) message
                    if (armed && msg.uuid == host.uuid && msg.deletionMode.toString() == mode) {
                        armed = false
                        preflightPassed.countDown()
                        assert resumeDelete.await(30, TimeUnit.SECONDS)
                    }
                }
            }, APIDeleteHostMsg.class)
            def pending = CompletableFuture.supplyAsync {
                new DeleteHostAction(uuid: host.uuid, deleteMode: mode,
                        sessionId: currentEnvSpec.session.uuid).call()
            }
            try {
                assert preflightPassed.await(30, TimeUnit.SECONDS)
                attachDataVolumeToVm { vmInstanceUuid = vm.uuid; volumeUuid = volume.uuid }
            } finally {
                armed = false
                resumeDelete.countDown()
            }
            def result = pending.get(30, TimeUnit.SECONDS)
            assert result.error != null
            assert result.error.details.contains(storageUuid)
            assert dbFindByUuid(host.uuid, HostVO.class).state == HostState.Enabled
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == host.uuid
            detachDataVolumeFromVm { uuid = volume.uuid }
        }
    }

    void testDeletionAdmission(HostInventory host, VmInstanceInventory vm, VolumeInventory volume) {
        HostDeletionGuard guard = new HostDeletionGuard()
        long deleting = guard.acquire(host.uuid, true)
        try {
            def result = new AttachDataVolumeToVmAction(vmInstanceUuid: vm.uuid, volumeUuid: volume.uuid,
                    sessionId: currentEnvSpec.session.uuid).call()
            assert result.error != null
            assert result.error.details.contains("host deletion is in progress")
            assert dbFindByUuid(volume.uuid, VolumeVO.class).vmInstanceUuid == null
        } finally {
            HostDeletionGuard.release(deleting)
        }

        long operation = guard.acquire(host.uuid, false)
        try {
            ["Permissive", "Enforcing"].each { mode ->
                def result = new DeleteHostAction(uuid: host.uuid, deleteMode: mode,
                        sessionId: currentEnvSpec.session.uuid).call()
                assert result.error != null
                assert result.error.details.contains("operation or another deletion is in progress")
                assert dbFindByUuid(host.uuid, HostVO.class).state == HostState.Enabled
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running
            }
        } finally {
            HostDeletionGuard.release(operation)
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
