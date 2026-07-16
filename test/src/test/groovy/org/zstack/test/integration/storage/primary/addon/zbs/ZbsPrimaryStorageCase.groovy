package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.volume.VolumeStats
import org.zstack.cbd.MdsUri
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.*
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsGlobalConfig
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Xingwei Yu
 * @date 2024/4/19 10:09
 */
class ZbsPrimaryStorageCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    ClusterInventory cluster
    PrimaryStorageInventory ps
    DiskOfferingInventory diskOffering
    VolumeInventory vol, vol2
    KVMHostInventory kvm

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
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(2)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.2"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    virtio = true
                }

                image {
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    format = "iso"
                    virtio = true
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm-3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachL2Network("l2")
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

                externalPrimaryStorage {
                    name = "zbs-1"
                    identity = "zbs"
                    defaultOutputProtocol = "CBD"
                    config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.2\",\"root:password@127.0.1.3\"],\"logicalPoolName\":\"lpool1\"}"
                    url = "fake url"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            zone = env.inventoryByName("zone") as ZoneInventory
            cluster = env.inventoryByName("cluster") as ClusterInventory
            ps = env.inventoryByName("zbs-1") as PrimaryStorageInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            kvm = env.inventoryByName("kvm-1") as KVMHostInventory

            testDefaultConfig()
            testUpdateExternalPrimaryStorage()
            testLifecycle()
            testDataVolumeLifecycle()
            testRevertVolumeSnapshotDoesNotBlockWhileWaitingForActiveClients()
            testRevertVolumeSnapshotFailsWhenActiveClientsAreNotReleasedBeforeTimeout()
            testMdsPing()
            testCheckHostStorageConnection()
            testNegativeScenario()
            testDataVolumeNegativeScenario()
            testDecodeMdsUriWithSpecialPassword()
        }
    }

    void testCheckHostStorageConnection() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        env.afterSimulator(ZbsStorageController.CHECK_HOST_STORAGE_CONNECTION_PATH) { rsp, HttpEntity<String> e ->
            ZbsStorageController.CheckHostStorageConnectionCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CheckHostStorageConnectionCmd)

            ZbsStorageController.CheckHostStorageConnectionRsp checkHostStorageConnectionRsp = new ZbsStorageController.CheckHostStorageConnectionRsp()
            checkHostStorageConnectionRsp.error = "fake error"
            checkHostStorageConnectionRsp.success = false

            return checkHostStorageConnectionRsp
        }

        expect(AssertionError.class) {
            reconnectHost {
                uuid = kvm.getUuid()
            }
        }

        retryInSecs {
            assert Q.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, kvm.getUuid())
                    .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                    .count() == 1
        }

        env.cleanSimulatorAndMessageHandlers()

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    void testUpdateExternalPrimaryStorage() {
        expect(AssertionError.class) {
            updateExternalPrimaryStorage {
                uuid = ps.uuid
                config = "{\"mdsUrls\":[],\"logicalPoolName\":\"lpool1\"}"
            }
        }

        expect(AssertionError.class) {
            updateExternalPrimaryStorage {
                uuid = ps.uuid
                config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.1\"],\"logicalPoolName\":\"lpool1\"}"
            }
        }

        updateExternalPrimaryStorage {
            uuid = ps.uuid
            config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.2\"],\"logicalPoolName\":\"lpool1\"}"
        }

        String addonInfo = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.addonInfo)
                .eq(ExternalPrimaryStorageVO_.uuid, ps.uuid)
                .findValue()
        assert !addonInfo.contains("127.0.1.3")

        updateExternalPrimaryStorage {
            uuid = ps.uuid
            config = "{\"mdsUrls\":[\"root:password@127.0.1.1:33\",\"root:password@127.0.1.2\"],\"logicalPoolName\":\"lpool1\"}"
        }

        addonInfo = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.addonInfo)
                .eq(ExternalPrimaryStorageVO_.uuid, ps.uuid)
                .findValue()
        assert addonInfo.contains("\"port\":33,\"addr\":\"127.0.1.1\"")

        updateExternalPrimaryStorage {
            uuid = ps.uuid
            config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.2\",\"root:password@127.0.1.3\"],\"logicalPoolName\":\"lpool1\"}"
        }

        addonInfo = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.addonInfo)
                .eq(ExternalPrimaryStorageVO_.uuid, ps.uuid)
                .findValue()
        assert addonInfo.contains("127.0.1.3")
    }

    void testDefaultConfig() {
        def rc = getResourceConfig {
            category = VolumeGlobalConfig.CATEGORY
            name = VolumeGlobalConfig.VOLUME_PHYSICAL_BLOCK_SIZE.getName()
            resourceUuid = ps.uuid
        } as GetResourceConfigResult
        assert rc.value == ZbsConstants.VOLUME_PHYSICAL_BLOCK_SIZE
    }

    void testLifecycle() {
        updateExternalPrimaryStorage {
            uuid = ps.uuid
            name = "test-zbs-new-name"
        }

        ps = queryPrimaryStorage {}[0] as ExternalPrimaryStorageInventory
        assert ps.name == "test-zbs-new-name"
        assert ps.url == ZbsConstants.ZBS_CBD_PREFIX_SCHEME + ps.uuid

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        env.afterSimulator(ZbsStorageController.DEPLOY_CLIENT_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeployClientCmd)

            ZbsStorageController.DeployClientRsp deployClientRsp = new ZbsStorageController.DeployClientRsp()
            if (cmd.ip.equals("127.0.0.1")) {
                deployClientRsp.success = false
                deployClientRsp.error = "on purpose"
            }

            return deployClientRsp
        }

        expect(AssertionError.class) {
            reconnectPrimaryStorage {
                uuid = ps.uuid
            }
        }

        env.cleanAfterSimulatorHandlers()

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    void testMdsPing() {
        PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(1)

        Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected

        def addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"clusterInfo\":{\"uuid\":\"123456789\",\"version\":\"1.6.1-for-test\"},\"mdsInfos\":[{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.1\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.2\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.3\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"}],\"logicalPoolInfos\":[{\"physicalPoolID\":1,\"redundanceAndPlaceMentPolicy\":{\"copysetNum\":300,\"replicaNum\":3,\"zoneNum\":3},\"logicalPoolID\":1,\"usedSize\":322961408,\"quota\":0,\"createTime\":1735875794,\"type\":0,\"rawWalUsedSize\":0,\"allocateStatus\":0,\"rawUsedSize\":968884224,\"physicalPoolName\":\"pool1\",\"capacity\":579933831168,\"logicalPoolName\":\"lpool1\",\"userPolicy\":\"eyJwb2xpY3kiIDogMX0=\",\"allocatedSize\":3221225472}]}"

        env.afterSimulator(ZbsPrimaryStorageMdsBase.PING_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsPrimaryStorageMdsBase.PingCmd.class)
            ZbsPrimaryStorageMdsBase.PingRsp pingRsp = new ZbsPrimaryStorageMdsBase.PingRsp()
            if (cmd.addr.equals("127.0.1.1")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            }

            return pingRsp
        }

        sleep(2000)

        addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"clusterInfo\":{\"uuid\":\"123456789\",\"version\":\"1.6.1-for-test\"},\"mdsInfos\":[{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.1\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.2\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.3\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"}],\"logicalPoolInfos\":[{\"physicalPoolID\":1,\"redundanceAndPlaceMentPolicy\":{\"copysetNum\":300,\"replicaNum\":3,\"zoneNum\":3},\"logicalPoolID\":1,\"usedSize\":322961408,\"quota\":0,\"createTime\":1735875794,\"type\":0,\"rawWalUsedSize\":0,\"allocateStatus\":0,\"rawUsedSize\":968884224,\"physicalPoolName\":\"pool1\",\"capacity\":579933831168,\"logicalPoolName\":\"lpool1\",\"userPolicy\":\"eyJwb2xpY3kiIDogMX0=\",\"allocatedSize\":3221225472}]}"

        assert Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected

        env.afterSimulator(ZbsPrimaryStorageMdsBase.PING_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsPrimaryStorageMdsBase.PingCmd.class)
            ZbsPrimaryStorageMdsBase.PingRsp pingRsp = new ZbsPrimaryStorageMdsBase.PingRsp()
            if (cmd.addr.equals("127.0.1.1")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            } else if (cmd.addr.equals("127.0.1.2")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            } else if (cmd.addr.equals("127.0.1.3")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            }

            return pingRsp
        }

        sleep(2000)

        addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"clusterInfo\":{\"uuid\":\"123456789\",\"version\":\"1.6.1-for-test\"},\"mdsInfos\":[{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.1\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.2\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.3\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"}],\"logicalPoolInfos\":[{\"physicalPoolID\":1,\"redundanceAndPlaceMentPolicy\":{\"copysetNum\":300,\"replicaNum\":3,\"zoneNum\":3},\"logicalPoolID\":1,\"usedSize\":322961408,\"quota\":0,\"createTime\":1735875794,\"type\":0,\"rawWalUsedSize\":0,\"allocateStatus\":0,\"rawUsedSize\":968884224,\"physicalPoolName\":\"pool1\",\"capacity\":579933831168,\"logicalPoolName\":\"lpool1\",\"userPolicy\":\"eyJwb2xpY3kiIDogMX0=\",\"allocatedSize\":3221225472}]}"

        assert Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Disconnected

        env.cleanAfterSimulatorHandlers()

        sleep(2000)

        Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected

        PrimaryStorageGlobalConfig.PING_INTERVAL.resetValue()
    }

    void testDataVolumeLifecycle() {
        vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        deleteVolume(vol.uuid)
    }

    void testRevertVolumeSnapshotDoesNotBlockWhileWaitingForActiveClients() {
        VolumeInventory volume = null
        VolumeSnapshotInventory snapshot = null
        AtomicBoolean clientsReleased = new AtomicBoolean(false)
        AtomicInteger rollbackCount = new AtomicInteger()
        CountDownLatch revertMethodReturned = new CountDownLatch(1)
        CountDownLatch completionDone = new CountDownLatch(1)
        AtomicReference<VolumeStats> volumeStats = new AtomicReference<>()
        AtomicReference<ErrorCode> errorCode = new AtomicReference<>()
        Thread revertThread = null

        try {
            volume = createDataVolume {
                name = "test-revert-waits-active-client-release-async"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            } as VolumeInventory

            snapshot = createVolumeSnapshot {
                name = "test-revert-waits-active-client-release-async"
                volumeUuid = volume.uuid
            } as VolumeSnapshotInventory

            String volumeInstallPath = volume.installPath

            env.simulator(ZbsStorageController.GET_VOLUME_CLIENTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetVolumeClientsCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetVolumeClientsCmd.class)
                assert cmd.path == volumeInstallPath: "revert must query active clients by the target volume install path"

                ZbsStorageController.GetVolumeClientsRsp rsp = new ZbsStorageController.GetVolumeClientsRsp()
                if (!clientsReleased.get()) {
                    rsp.clients = [new ZbsStorageController.ClientInfo("127.0.0.1", 7700)]
                }
                return rsp
            }

            env.simulator(ZbsStorageController.ROLLBACK_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.RollbackSnapshotCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.RollbackSnapshotCmd.class)
                assert cmd.path == snapshot.primaryStorageInstallPath: "rollback must use the requested snapshot install path"
                assert clientsReleased.get(): "rollback must not run before active clients are released"
                rollbackCount.incrementAndGet()

                ZbsStorageController.RollbackSnapshotRsp rsp = new ZbsStorageController.RollbackSnapshotRsp()
                rsp.installPath = volumeInstallPath
                rsp.size = volume.size
                rsp.actualSize = volume.actualSize
                return rsp
            }

            ZbsStorageController controller = bean(ExternalPrimaryStorageFactory.class).getControllerSvc(ps.uuid) as ZbsStorageController
            revertThread = Thread.start {
                try {
                    controller.revertVolumeSnapshot(snapshot.primaryStorageInstallPath, new ReturnValueCompletion<VolumeStats>(null) {
                        @Override
                        void success(VolumeStats returnValue) {
                            volumeStats.set(returnValue)
                            completionDone.countDown()
                        }

                        @Override
                        void fail(ErrorCode error) {
                            errorCode.set(error)
                            completionDone.countDown()
                        }
                    })
                } finally {
                    revertMethodReturned.countDown()
                }
            }

            assert revertMethodReturned.await(2, TimeUnit.SECONDS): "revertVolumeSnapshot must return without synchronously sleeping while active clients remain"
            assert rollbackCount.get() == 0: "rollback must not run before active clients are released"
            assert completionDone.getCount() == 1: "completion must wait for active clients to be released"

            clientsReleased.set(true)

            assert completionDone.await(5, TimeUnit.SECONDS): "completion must finish after active clients are released"
            assert errorCode.get() == null: "revert must not fail after active clients are released"
            assert volumeStats.get() != null: "revert must return volume stats after rollback"
            assert rollbackCount.get() == 1: "rollback must run exactly once after active clients are released"
        } finally {
            clientsReleased.set(true)
            if (revertThread != null && revertThread.isAlive()) {
                revertThread.interrupt()
                revertThread.join(5000)
            }
            if (snapshot != null) {
                deleteVolumeSnapshot {
                    uuid = snapshot.uuid
                }
            }
            if (volume != null) {
                deleteVolume(volume.uuid)
            }

            env.cleanSimulatorAndMessageHandlers()
        }
    }

    void testRevertVolumeSnapshotFailsWhenActiveClientsAreNotReleasedBeforeTimeout() {
        VolumeInventory volume = null
        VolumeSnapshotInventory snapshot = null
        AtomicInteger rollbackCount = new AtomicInteger()
        CountDownLatch completionDone = new CountDownLatch(1)
        AtomicReference<ErrorCode> errorCode = new AtomicReference<>()

        try {
            ZbsGlobalConfig.VOLUME_CLIENT_RELEASE_TIMEOUT.updateValue(1)

            volume = createDataVolume {
                name = "test-revert-fails-when-active-client-not-released"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            } as VolumeInventory

            snapshot = createVolumeSnapshot {
                name = "test-revert-fails-when-active-client-not-released"
                volumeUuid = volume.uuid
            } as VolumeSnapshotInventory

            String volumeInstallPath = volume.installPath

            env.simulator(ZbsStorageController.GET_VOLUME_CLIENTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetVolumeClientsCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetVolumeClientsCmd.class)
                assert cmd.path == volumeInstallPath: "revert must keep polling the target volume install path before timeout"

                ZbsStorageController.GetVolumeClientsRsp rsp = new ZbsStorageController.GetVolumeClientsRsp()
                rsp.clients = [new ZbsStorageController.ClientInfo("127.0.0.1", 7700)]
                return rsp
            }

            env.simulator(ZbsStorageController.ROLLBACK_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                rollbackCount.incrementAndGet()
                return new ZbsStorageController.RollbackSnapshotRsp()
            }

            ZbsStorageController controller = bean(ExternalPrimaryStorageFactory.class).getControllerSvc(ps.uuid) as ZbsStorageController
            controller.revertVolumeSnapshot(snapshot.primaryStorageInstallPath, new ReturnValueCompletion<VolumeStats>(null) {
                @Override
                void success(VolumeStats returnValue) {
                    completionDone.countDown()
                }

                @Override
                void fail(ErrorCode error) {
                    errorCode.set(error)
                    completionDone.countDown()
                }
            })

            assert completionDone.await(5, TimeUnit.SECONDS): "revert must fail when active clients are not released before the configured timeout"
            assert errorCode.get() != null: "timeout must fail the completion"
            assert errorCode.get().details.contains("were not released within 1 seconds")
            assert rollbackCount.get() == 0: "rollback must not run after client-release wait times out"
        } finally {
            ZbsGlobalConfig.VOLUME_CLIENT_RELEASE_TIMEOUT.resetValue()
            if (snapshot != null) {
                deleteVolumeSnapshot {
                    uuid = snapshot.uuid
                }
            }
            if (volume != null) {
                deleteVolume(volume.uuid)
            }

            env.cleanSimulatorAndMessageHandlers()
        }
    }

    void testNegativeScenario() {
        expect(AssertionError.class) {
            addExternalPrimaryStorage {
                zoneUuid = zone.uuid
                name = "zbs-2"
                identity = "zbs"
                defaultOutputProtocol = "CBD"
                config = "{\"mdsUrls\":[\"root:password@127.0.2.1\",\"root:password@127.0.2.2\",\"root:password@127.0.2.3\"],\"logicalPoolName\":\"lpo/ol1\"}"
                url = ""
            }
        }

        env.simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            throw new HttpError(404, "on purpose")
        }

        expect(AssertionError.class) {
            addExternalPrimaryStorage {
                zoneUuid = zone.uuid
                name = "zbs-2"
                identity = "zbs"
                defaultOutputProtocol = "CBD"
                config = "{\"mdsUrls\":[\"root:password@127.0.2.1\",\"root:password@127.0.2.2\",\"root:password@127.0.2.3\"],\"logicalPoolName\":\"lpool1\"}"
                url = ""
            }
        }

        env.simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return [:]
        }

        expect(AssertionError.class) {
            addExternalPrimaryStorage {
                zoneUuid = zone.uuid
                name = "zbs-2"
                identity = "zbs"
                defaultOutputProtocol = "CBD"
                config = "{\"mdsUrls\":[\"root:password@127.0.2.1\",\"root:password@127.0.2.2\",\"root:password@127.0.2.3\"],\"logicalPoolName\":\"lpool1\"}"
                url = ""
            }
        }

        env.simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return [:]
        }

        env.simulator(ZbsStorageController.GET_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.GetCapacityRsp()
            rsp.setSuccess(false)
            rsp.setError("failed to GET_CAPACITY on purpose")
            return rsp
        }

        expect(AssertionError.class) {
            addExternalPrimaryStorage {
                zoneUuid = zone.uuid
                name = "zbs-2"
                identity = "zbs"
                defaultOutputProtocol = "CBD"
                config = "{\"mdsUrls\":[\"root:password@127.0.2.1\",\"root:password@127.0.2.2\",\"root:password@127.0.2.3\"],\"logicalPoolName\":\"lpool1\"}"
                url = ""
            }
        }
    }

    void testDataVolumeNegativeScenario() {
        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.CreateVolumeRsp()
            rsp.setSuccess(false)
            rsp.setError("failed to CREATE_VOLUME on purpose")
            return rsp
        }

        expect(AssertionError.class) {
            vol2 = createDataVolume {
                name = "test-2"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            } as VolumeInventory
        }

        def actualSize = SizeUnit.GIGABYTE.toByte(1)

        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.CreateVolumeRsp()
            rsp.size = actualSize
            rsp.actualSize = actualSize
            rsp.installPath = "cbd:pool1/lpool1/test2"
            return rsp
        }

        vol2 = createDataVolume {
            name = "test-2"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        env.simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.DeleteVolumeRsp()
            rsp.setSuccess(false)
            rsp.setError("failed to DELETE_VOLUME on purpose")
            return rsp
        }

        expect(AssertionError.class) {
            deleteVolume(vol2.uuid)
        }
    }

    void testDecodeMdsUriWithSpecialPassword() {
        def specialPassword = "password123-`=[];,./~!@#\$%^&*()_+|{}:<>?"
        def mdsUri = "root:${specialPassword}@127.0.2.1"
        MdsUri uri = new MdsUri(mdsUri);
        assert uri.password == specialPassword
    }


    void deleteVolume(String volUuid) {
        deleteDataVolume {
            uuid = volUuid
        }

        expungeDataVolume {
            uuid = volUuid
        }
    }
}
