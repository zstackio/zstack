package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostProtocolRefVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostProtocolRefVO_
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.volume.VolumeProtocol
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ZbsVhostAttachDeployFailureCase extends SubCase {
    EnvSpec env
    CloudBus bus
    PrimaryStorageInventory ps
    ClusterInventory cluster
    HostInventory kvm1, kvm2
    AtomicInteger deploymentCalls = new AtomicInteger(0)

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
            }

            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
                    name = "zbs-vhost"
                    identity = "zbs"
                    defaultOutputProtocol = "Vhost"
                    config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.2\",\"root:password@127.0.1.3\"],\"logicalPoolName\":\"lpool1\"}"
                    url = "zbs"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            ps = env.inventoryByName("zbs-vhost") as PrimaryStorageInventory
            cluster = env.inventoryByName("cluster") as ClusterInventory
            kvm1 = env.inventoryByName("kvm1") as HostInventory
            kvm2 = env.inventoryByName("kvm2") as HostInventory
            bus = bean(CloudBus.class)

            ["/zbs/primarystorage/vhost/deploy", "/zbs/primarystorage/vhost/destroy",
             "/zbs/primarystorage/vhost/target/prepareenv"].each { path ->
                env.simulator(path) { HttpEntity<String> e, EnvSpec spec ->
                    deploymentCalls.incrementAndGet()
                    return new ZbsStorageController.AgentResponse()
                }
            }

            testAttachFailsWhenDeployFailureRatioReachesThreshold()
            testAttachSucceedsBelowThresholdThenSelfHeals()
            assert deploymentCalls.get() == 0 : \
                    "ZStone owns vhost deployment: expected no deployment calls, actual=${deploymentCalls.get()}"
        }
    }

    // one of two hosts fails vhost check -> 50% > default 0.3 threshold -> attach fails
    void testAttachFailsWhenDeployFailureRatioReachesThreshold() {
        AtomicReference<String> failHostIp = new AtomicReference<>("127.0.0.2")
        registerBaseStubs(failHostIp)

        expect(AssertionError.class) {
            attachPrimaryStorageToCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = cluster.uuid
            }
        }
    }

    // raise threshold to 0.6 so 50% < 0.6 -> attach succeeds; failed host stays Disconnected then self-heals
    void testAttachSucceedsBelowThresholdThenSelfHeals() {
        updateGlobalConfig {
            category = "externalPrimaryStorage"
            name = "attach.hostDeployFailureRatioThreshold"
            value = 0.6
        }

        AtomicReference<String> failHostIp = new AtomicReference<>("127.0.0.2")
        registerBaseStubs(failHostIp)

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, kvm2.uuid)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .isExists() : "failed host must be recorded Disconnected"

        assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, kvm1.uuid)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                .isExists() : "healthy host must be Connected"

        failHostIp.set(null)
        updateGlobalConfig {
            category = "host"
            name = "ping.interval"
            value = 1
        }

        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, kvm2.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists() : "periodic ping did not self-heal the previously failed host"
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    void registerBaseStubs(AtomicReference<String> failHostIp) {
        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus b ->
            b.reply(msg, new UploadImageToRemoteTargetReply())
        }

        env.simulator(ZbsStorageController.CHECK_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CheckVhostCmd.class)
            if (cmd.hostIp == failHostIp.get()) {
                def rsp = new ZbsStorageController.AgentResponse()
                rsp.success = false
                rsp.error = "vhost target is not ready in ZStone for host ${cmd.hostIp}"
                return rsp
            }
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.VHOST_TARGET_HEALTH_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.VhostTargetHealthRsp()
            rsp.targetRunning = true
            return rsp
        }
        env.afterSimulator(ZbsStorageController.CREATE_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd)
            if (cmd.volume == ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME) {
                def vrsp = new ZbsStorageController.CreateVolumeRsp()
                vrsp.installPath = "zbs://${cmd.logicalPool}/${cmd.volume}".toString()
                return vrsp
            }
            return rsp
        }
    }
}
