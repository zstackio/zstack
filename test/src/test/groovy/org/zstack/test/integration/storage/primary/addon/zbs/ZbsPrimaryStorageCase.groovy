package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.sdk.*
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

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
                        name = "kvm"
                        managementIp = "localhost"
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
                    url = ""
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

            testZbsStorageLifecycle()
            testDataVolumeLifecycle()
            PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(1)
            testZbsPrimaryStorageMdsPing()
            PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(60)
            testZbsStorageNegativeScenario()
            testDataVolumeNegativeScenario()
        }
    }

    void testZbsStorageLifecycle() {
        updateExternalPrimaryStorage {
            uuid = ps.uuid
            name = "test-zbs-new-name"
        }

        ps = queryPrimaryStorage {}[0] as ExternalPrimaryStorageInventory
        assert ps.name == "test-zbs-new-name"

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    void testZbsPrimaryStorageMdsPing() {
        Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected

        def addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"mdsInfos\":[{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.1\",\"mdsExternalAddr\":\"1.1.1.1:6666\",\"mdsStatus\":\"Connected\"},{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.2\",\"mdsExternalAddr\":\"1.1.1.2:6666\",\"mdsStatus\":\"Connected\"},{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.3\",\"mdsExternalAddr\":\"1.1.1.3:6666\",\"mdsStatus\":\"Connected\"}]}"

        env.afterSimulator(ZbsPrimaryStorageMdsBase.PING_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsPrimaryStorageMdsBase.PingCmd.class)
            ZbsPrimaryStorageMdsBase.PingRsp pingRsp = new ZbsPrimaryStorageMdsBase.PingRsp()
            if (cmd.mdsExternalAddr.equals("1.1.1.1:6666")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            }

            return pingRsp
        }

        sleep(2000)

        addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"mdsInfos\":[{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.1\",\"mdsExternalAddr\":\"1.1.1.1:6666\",\"mdsStatus\":\"Disconnected\"},{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.2\",\"mdsExternalAddr\":\"1.1.1.2:6666\",\"mdsStatus\":\"Connected\"},{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.3\",\"mdsExternalAddr\":\"1.1.1.3:6666\",\"mdsStatus\":\"Connected\"}]}"

        assert Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected

        env.afterSimulator(ZbsPrimaryStorageMdsBase.PING_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsPrimaryStorageMdsBase.PingCmd.class)
            ZbsPrimaryStorageMdsBase.PingRsp pingRsp = new ZbsPrimaryStorageMdsBase.PingRsp()
            if (cmd.mdsExternalAddr.equals("1.1.1.1:6666")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            } else if (cmd.mdsExternalAddr.equals("1.1.1.2:6666")) {
                pingRsp.success = false
                pingRsp.error = "on purpose"
            }

            return pingRsp
        }

        sleep(1000)

        addonInfo = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.addonInfo).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue()

        assert addonInfo == "{\"mdsInfos\":[{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.1\",\"mdsExternalAddr\":\"1.1.1.1:6666\",\"mdsStatus\":\"Disconnected\"},{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.2\",\"mdsExternalAddr\":\"1.1.1.2:6666\",\"mdsStatus\":\"Disconnected\"},{\"sshUsername\":\"root\",\"sshPassword\":\"password\",\"sshPort\":22,\"mdsAddr\":\"127.0.1.3\",\"mdsExternalAddr\":\"1.1.1.3:6666\",\"mdsStatus\":\"Connected\"}]}"

        assert Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Disconnected

        env.cleanAfterSimulatorHandlers()

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.status).eq(ExternalPrimaryStorageVO_.uuid, ps.uuid).findValue() == PrimaryStorageStatus.Connected
    }

    void testDataVolumeLifecycle() {
        vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        deleteVolume(vol.uuid)
    }

    void testZbsStorageNegativeScenario() {
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

        env.simulator(ZbsStorageController.GET_FACTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.GetFactsRsp()
            rsp.setSuccess(false)
            rsp.setError("failed to GET_FACTS on purpose")
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

        env.simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return [:]
        }

        env.simulator(ZbsStorageController.GET_FACTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
            ZbsStorageController.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetFactsCmd.class)

            def rsp = new ZbsStorageController.GetFactsRsp()
            if (cmd.getMdsAddr().equals("127.0.2.1")) {
                rsp.setMdsExternalAddr("1.1.2.1:6666")
            } else if (cmd.mdsAddr.equals("127.0.2.2")) {
                rsp.setMdsExternalAddr("1.1.2.2:6666")
            } else if (cmd.mdsAddr.equals("127.0.2.3")) {
                rsp.setMdsExternalAddr("1.1.2.3:6666")
            }

            return rsp
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

    void deleteVolume(String volUuid) {
        deleteDataVolume {
            uuid = volUuid
        }

        expungeDataVolume {
            uuid = volUuid
        }
    }
}
