package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.Q
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.cbd.MdsUri
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.*
import org.zstack.storage.addon.primary.ExternalPrimaryStorageCanonicalEvent
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.atomic.AtomicBoolean

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
    EventFacade evtf

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
            evtf = bean(EventFacade.class)

            testDefaultConfig()
            testUpdateExternalPrimaryStorage()
            testLifecycle()
            testDataVolumeLifecycle()
            testMdsPing()
            testCheckHostStorageConnection()
            testNegativeScenario()
            testAddExternalPrimaryStorageWithMalformedJsonRejectedByInterceptor()
            testDataVolumeNegativeScenario()
            testDecodeMdsUriWithSpecialPassword()
        }
    }

    void testCheckHostStorageConnection() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        Set<String> heartbeatPools = Collections.synchronizedSet(new HashSet<>())
        AtomicBoolean checkStartedBeforeHeartbeatReady = new AtomicBoolean(false)
        env.afterSimulator(ZbsStorageController.CREATE_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            ZbsStorageController.CreateVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd)
            if (cmd.volume == ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME) {
                heartbeatPools.add(cmd.logicalPool)
                ZbsStorageController.CreateVolumeRsp createVolumeRsp = new ZbsStorageController.CreateVolumeRsp()
                createVolumeRsp.installPath = "zbs://${cmd.logicalPool}/${cmd.volume}"
                return createVolumeRsp
            }

            return rsp
        }

        env.afterSimulator(ZbsStorageController.CHECK_HOST_STORAGE_CONNECTION_PATH) { rsp, HttpEntity<String> e ->
            ZbsStorageController.CheckHostStorageConnectionCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CheckHostStorageConnectionCmd)
            if (!heartbeatPools.contains("lpool1")) {
                checkStartedBeforeHeartbeatReady.set(true)
            }

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
            assert heartbeatPools.contains("lpool1")
            assert !checkStartedBeforeHeartbeatReady.get()
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
        ExternalPrimaryStorageCanonicalEvent.AddonInfoChangedData data = null
        long count = 0
        EventCallback<ExternalPrimaryStorageCanonicalEvent.AddonInfoChangedData> callback = new EventCallback<ExternalPrimaryStorageCanonicalEvent.AddonInfoChangedData>() {
            @Override
            protected void run(Map<String, String> tokens, ExternalPrimaryStorageCanonicalEvent.AddonInfoChangedData d) {
                data = d
                count++
            }
        }

        evtf.on(ExternalPrimaryStorageCanonicalEvent.ADDON_INFO_CHANGED_PATH, callback)
        PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(1)

        Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected

        def addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"clusterInfo\":{\"uuid\":\"123456789\",\"version\":\"1.6.1-for-test\"},\"mdsInfos\":[{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.1\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.2\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.3\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Connected\"}],\"logicalPoolInfos\":[{\"physicalPoolID\":1,\"redundanceAndPlaceMentPolicy\":{\"copysetNum\":300,\"replicaNum\":3,\"zoneNum\":3},\"logicalPoolID\":1,\"usedSize\":322961408,\"quota\":0,\"createTime\":1735875794,\"type\":0,\"rawWalUsedSize\":0,\"allocateStatus\":0,\"rawUsedSize\":968884224,\"physicalPoolName\":\"pool1\",\"capacity\":579933831168,\"logicalPoolName\":\"lpool1\",\"userPolicy\":\"eyJwb2xpY3kiIDogMX0=\",\"allocatedSize\":3221225472}]}"
        assert data == null

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
        assert count == 1
        assert data.uuid == ps.uuid

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

        sleep(2500)

        addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"clusterInfo\":{\"uuid\":\"123456789\",\"version\":\"1.6.1-for-test\"},\"mdsInfos\":[{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.1\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.2\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"},{\"username\":\"root\",\"password\":\"password\",\"port\":22,\"addr\":\"127.0.1.3\",\"externalAddr\":\"127.0.0.1\",\"status\":\"Disconnected\"}],\"logicalPoolInfos\":[{\"physicalPoolID\":1,\"redundanceAndPlaceMentPolicy\":{\"copysetNum\":300,\"replicaNum\":3,\"zoneNum\":3},\"logicalPoolID\":1,\"usedSize\":322961408,\"quota\":0,\"createTime\":1735875794,\"type\":0,\"rawWalUsedSize\":0,\"allocateStatus\":0,\"rawUsedSize\":968884224,\"physicalPoolName\":\"pool1\",\"capacity\":579933831168,\"logicalPoolName\":\"lpool1\",\"userPolicy\":\"eyJwb2xpY3kiIDogMX0=\",\"allocatedSize\":3221225472}]}"

        assert Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Disconnected
        assert count == 2
        assert data.uuid == ps.uuid

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

    void testAddExternalPrimaryStorageWithMalformedJsonRejectedByInterceptor() {
        long extPsCountBefore = Q.New(ExternalPrimaryStorageVO.class).count()
        long primaryStorageCountBefore = Q.New(PrimaryStorageVO.class).count()
        long protocolRefCountBefore = Q.New(PrimaryStorageOutputProtocolRefVO.class).count()

        expect(AssertionError.class) {
            addExternalPrimaryStorage {
                zoneUuid = zone.uuid
                name = "zbs-bad-json"
                identity = "zbs"
                defaultOutputProtocol = "CBD"
                config = "{this is not valid json"
                url = ""
            }
        }

        long extPsCountAfter = Q.New(ExternalPrimaryStorageVO.class).count()
        long primaryStorageCountAfter = Q.New(PrimaryStorageVO.class).count()
        long protocolRefCountAfter = Q.New(PrimaryStorageOutputProtocolRefVO.class).count()
        assert extPsCountAfter == extPsCountBefore : \
                "ExternalPrimaryStorageVO leaked despite marshal-time JSON rejection: " +
                "before=${extPsCountBefore} after=${extPsCountAfter}"
        assert primaryStorageCountAfter == primaryStorageCountBefore : \
                "PrimaryStorageVO leaked despite marshal-time JSON rejection: " +
                "before=${primaryStorageCountBefore} after=${primaryStorageCountAfter}"
        assert protocolRefCountAfter == protocolRefCountBefore : \
                "PrimaryStorageOutputProtocolRefVO leaked despite marshal-time JSON rejection: " +
                "before=${protocolRefCountBefore} after=${protocolRefCountAfter}"
        boolean stalePsByName = Q.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.name, "zbs-bad-json")
                .isExists()
        assert !stalePsByName : "stale ExternalPrimaryStorageVO[name=zbs-bad-json] reached DB despite interceptor rejection"

        def psList = queryPrimaryStorage {} as List
        assert psList != null : "queryPrimaryStorage threw / returned null after attempted bad-JSON Add"
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
