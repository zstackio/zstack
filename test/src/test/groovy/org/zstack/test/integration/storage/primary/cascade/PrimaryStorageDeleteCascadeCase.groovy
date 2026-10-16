package org.zstack.test.integration.storage.primary.cascade

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.APIDeletePrimaryStorageMsg
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.header.volume.VolumeStatus
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.DeletePrimaryStorageAction
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/5/10.
 */
class PrimaryStorageDeleteCascadeCase extends SubCase {
    EnvSpec env
    L3NetworkInventory l3

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
                }

                cluster {
                    name = "cluster3"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
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

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm2")
            }

            vm {
                name = "vm3"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            l3 = env.inventoryByName("l3")
            GetIpAddressCapacityResult capacityResult = getIpAddressCapacity {
                l3NetworkUuids = [l3.uuid]
            }

            testPrimaryStorageDeleteCascadeWhenUseLocalStorage()
            testPrimaryStorageDeleteCascadeWhenUseSMP()
            testPrimaryStorageDeleteCascadeWhenUseNfs()

            retryInSecs {
                GetIpAddressCapacityResult result = getIpAddressCapacity {
                    l3NetworkUuids = [l3.uuid]
                }
                assert capacityResult.availableCapacity + 4 == result.availableCapacity
            }
        }
    }

    void testPrimaryStorageDeleteCascadeWhenUseLocalStorage() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm1")
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster1")

        VolumeInventory volume = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.getUuid()
            volumeUuid = volume.uuid
        }

        // detach cascade will set vm to stopped
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.getUuid()
            clusterUuid = cluster.getUuid()
        }
        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
        }

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // delete primary storage cascade will destroy vm and data volume using Keep volume strategy
        deletePrimaryStorage {
            uuid = ps.getUuid()
        }
        VolumeVO volVO
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            volVO = dbFindByUuid(volume.getUuid(), VolumeVO.class)

            return {
                assert vo == null
                assert volVO == null
            }
        }
        assert destroyCmd == null
    }

    void testPrimaryStorageDeleteCascadeWhenUseSMP() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm2")
        PrimaryStorageInventory ps = env.inventoryByName("smp")
        ClusterInventory cluster = env.inventoryByName("cluster2")

        VolumeInventory volume = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.getUuid()
            volumeUuid = volume.uuid
        }

        assertNonLocalDeleteRejected(ps)

        // detach cascade will set vm to stopped
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.getUuid()
            clusterUuid = cluster.getUuid()
        }
        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
        }

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }
        assertNonLocalDeleteRejected(ps)
        assert destroyCmd == null
        removeVolumesBeforeDeletingStorage(ps, cluster, vm, volume)
        destroyCmd = null
        // Non-local storage must be empty before direct deletion.
        deletePrimaryStorage {
            uuid = ps.getUuid()
        }
        VolumeVO volVO
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            volVO = dbFindByUuid(volume.getUuid(), VolumeVO.class)

            assert vo == null
            assert volVO == null
        }
        assert destroyCmd == null
    }

    void testPrimaryStorageDeleteCascadeWhenUseNfs() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm3")
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        ClusterInventory cluster = env.inventoryByName("cluster3")

        VolumeInventory volume = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.getUuid()
            volumeUuid = volume.uuid
        }

        assertNonLocalDeleteRejected(ps)

        // detach cascade will set vm to stopped
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.getUuid()
            clusterUuid = cluster.getUuid()
        }
        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
        }

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }
        assertNonLocalDeleteRejected(ps)
        assert destroyCmd == null
        removeVolumesBeforeDeletingStorage(ps, cluster, vm, volume)
        destroyCmd = null
        // An empty non-local storage can also be deleted in enforcing mode.
        deletePrimaryStorage {
            uuid = ps.getUuid()
            deleteMode = "Enforcing"
        }
        VolumeVO volVO
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            volVO = dbFindByUuid(volume.getUuid(), VolumeVO.class)
            assert vo == null
            assert volVO == null
        }
        assert destroyCmd == null
    }

    void assertNonLocalDeleteRejected(PrimaryStorageInventory ps, String volumeUuid = null) {
        def state = dbFindByUuid(ps.uuid, PrimaryStorageVO.class).state
        List<String> before = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, ps.uuid)
                .select(VolumeVO_.uuid).listValues().sort()
        ["Permissive", "Enforcing"].each { mode ->
            def result = new DeletePrimaryStorageAction(uuid: ps.uuid, deleteMode: mode,
                    sessionId: currentEnvSpec.session.uuid).call()
            assert result.error != null
            assert result.error.details.contains("cannot delete non-local primary storage")
            assert result.error.details.contains(ps.uuid)
            if (volumeUuid != null) {
                assert result.error.details.contains(volumeUuid)
            }
            assert dbFindByUuid(ps.uuid, PrimaryStorageVO.class).state == state
            assert Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, ps.uuid)
                    .select(VolumeVO_.uuid).listValues().sort() == before

        }
    }


    void testVolumeAppearsAfterApiPreflight(PrimaryStorageInventory ps) {
        ["Permissive", "Enforcing"].each { mode ->
            assert !Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, ps.uuid).isExists()
            CountDownLatch accepted = new CountDownLatch(1)
            CountDownLatch resume = new CountDownLatch(1)
            AtomicBoolean armed = new AtomicBoolean(true)
            bean(CloudBus.class).installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
                @Override
                void beforeDeliveryMessage(Message message) {
                    APIDeletePrimaryStorageMsg msg = message as APIDeletePrimaryStorageMsg
                    if (msg.uuid == ps.uuid && msg.deletionMode.toString() == mode && armed.compareAndSet(true, false)) {
                        accepted.countDown()
                        assert resume.await(60, TimeUnit.SECONDS)
                    }
                }
            }, APIDeletePrimaryStorageMsg.class)
            def pending = CompletableFuture.supplyAsync {
                new DeletePrimaryStorageAction(uuid: ps.uuid, deleteMode: mode,
                        sessionId: currentEnvSpec.session.uuid).call()
            }
            VolumeInventory volume
            def result
            try {
                assert accepted.await(30, TimeUnit.SECONDS)
                volume = createDataVolume {
                    name = "created-after-delete-preflight"
                    primaryStorageUuid = ps.uuid
                    diskOfferingUuid = env.inventoryByName("diskOffering").uuid
                }
            } finally {
                armed.set(false)
                resume.countDown()
                result = pending.get(45, TimeUnit.SECONDS)
            }
            assert result.error != null
            assert result.error.details.contains("cannot delete non-local primary storage")
            assert result.error.details.contains(volume.uuid)
            assert dbFindByUuid(ps.uuid, PrimaryStorageVO.class).state.toString() == "Enabled"
            assert dbFindByUuid(volume.uuid, VolumeVO.class).status == VolumeStatus.Ready
            deleteDataVolume { uuid = volume.uuid }
            if (dbFindByUuid(volume.uuid, VolumeVO.class) != null) {
                expungeDataVolume { uuid = volume.uuid }
            }
        }
    }

    void testInFlightVolumeBeforeStorageBinding(PrimaryStorageInventory ps) {
        CountDownLatch responseReady = new CountDownLatch(1)
        CountDownLatch resume = new CountDownLatch(1)
        String volumeUuid = UUID.randomUUID().toString().replace("-", "")
        env.afterSimulator(NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.CreateEmptyVolumeCmd.class)
            if (cmd.volumeUuid == volumeUuid) {
                responseReady.countDown()
                assert resume.await(60, TimeUnit.SECONDS)
            }
            return rsp
        }
        def future = CompletableFuture.supplyAsync {
            new CreateDataVolumeAction(name: "in-flight", resourceUuid: volumeUuid,
                    diskOfferingUuid: env.inventoryByName("diskOffering").uuid,
                    primaryStorageUuid: ps.uuid, sessionId: currentEnvSpec.session.uuid).call()
        }
        def result
        try {
            assert responseReady.await(30, TimeUnit.SECONDS)
            assert dbFindByUuid(volumeUuid, VolumeVO.class).primaryStorageUuid == null
            detachPrimaryStorageFromCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = env.inventoryByName("cluster3").uuid
            }
            ["Permissive", "Enforcing"].each { mode ->
                def result = new DeletePrimaryStorageAction(uuid: ps.uuid, deleteMode: mode,
                        sessionId: currentEnvSpec.session.uuid).call()
                assert result.error != null
                assert result.error.details.contains("unfinished volume operation")
            }
            attachPrimaryStorageToCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = env.inventoryByName("cluster3").uuid
            }
        } finally {
            resume.countDown()
            try {
                result = future.get(45, TimeUnit.SECONDS)
            } finally {
                env.afterSimulator(NfsPrimaryStorageKVMBackend.CREATE_EMPTY_VOLUME_PATH) { rsp, e -> rsp }
            }
        }
        result.throwExceptionIfError()
        assert dbFindByUuid(volumeUuid, VolumeVO.class).primaryStorageUuid == ps.uuid
        assertNonLocalDeleteRejected(ps, volumeUuid)
        deleteDataVolume { uuid = volumeUuid }
        if (dbFindByUuid(volumeUuid, VolumeVO.class) != null) {
            expungeDataVolume { uuid = volumeUuid }
        }
    }

    void removeVolumesBeforeDeletingStorage(PrimaryStorageInventory ps, ClusterInventory cluster,
                                           VmInstanceInventory vm, VolumeInventory volume) {
        attachPrimaryStorageToCluster { primaryStorageUuid = ps.uuid; clusterUuid = cluster.uuid }
        detachDataVolumeFromVm { uuid = volume.uuid }
        String policy = VmGlobalConfig.VM_DELETION_POLICY.value()
        try {
            VmGlobalConfig.VM_DELETION_POLICY.updateValue("Delay")
            destroyVmInstance { uuid = vm.uuid }
            expungeVmInstance { uuid = vm.uuid }
        } finally {
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(policy)
        }

        // The unattached data volume is now the only remaining volume.
        assertNonLocalDeleteRejected(ps, volume.uuid)
        String volumePolicy = VolumeGlobalConfig.VOLUME_DELETION_POLICY.value()
        try {
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue("Delay")
            deleteDataVolume { uuid = volume.uuid }
            assert dbFindByUuid(volume.uuid, VolumeVO.class).status == VolumeStatus.Deleted
            assertNonLocalDeleteRejected(ps, volume.uuid)
            expungeDataVolume { uuid = volume.uuid }
        } finally {
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(volumePolicy)
        }
        assert !Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, ps.uuid).isExists()
        if (ps.type == "NFS") {
            testInFlightVolumeBeforeStorageBinding(ps)
            testVolumeAppearsAfterApiPreflight(ps)
        }
        detachPrimaryStorageFromCluster { primaryStorageUuid = ps.uuid; clusterUuid = cluster.uuid }
    }
}
