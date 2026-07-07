package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.image.ImageVO
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_PRIMARY_10035

class ZbsVhostLegacyBootAllocateCase extends SubCase {
    EnvSpec env
    CloudBus bus
    PrimaryStorageInventory ps
    ClusterInventory cluster
    InstanceOfferingInventory instanceOffering
    ImageInventory image
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
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
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
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            bus = bean(CloudBus.class)

            registerBaseStubs()

            attachPrimaryStorageToCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = cluster.uuid
            }

            testLegacyImageRejectedWhenOnlyVhostPsCandidate()
            testUefiImageNotRejectedByLegacyBootFilter()

            detachPrimaryStorageFromCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = cluster.uuid
            }
        }
    }

    void testLegacyImageRejectedWhenOnlyVhostPsCandidate() {
        setImageBootMode(image.uuid, "Legacy")

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "legacy-vm"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.primaryStorageUuidForRootVolume = ps.uuid
        action.sessionId = adminSession()

        CreateVmInstanceAction.Result result = action.call()
        assert result.error != null : "Legacy image with only a ZBS-vhost candidate PS must fail at allocate stage"
        assert JSONObjectUtil.toJsonString(result.error).contains(ORG_ZSTACK_STORAGE_PRIMARY_10035) :
                "Legacy vm should be rejected by the primary storage feature allocator, actual: ${JSONObjectUtil.toJsonString(result.error)}"
    }

    void testUefiImageNotRejectedByLegacyBootFilter() {
        setImageBootMode(image.uuid, "UEFI")

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "uefi-vm"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.primaryStorageUuidForRootVolume = ps.uuid
        action.sessionId = adminSession()

        CreateVmInstanceAction.Result result = action.call()
        assert result.error == null ||
                !JSONObjectUtil.toJsonString(result.error).contains("returns zero primary storage candidate") :
                "UEFI image must not be rejected by the legacy boot allocator filter"
    }

    void setImageBootMode(String imageUuid, String bootMode) {
        def existing = querySystemTag {
            conditions = ["resourceUuid=${imageUuid}".toString(), "tag~=bootMode::%"]
        } as List<SystemTagInventory>

        if (existing.isEmpty()) {
            createSystemTag {
                resourceUuid = imageUuid
                resourceType = ImageVO.getSimpleName()
                tag = "bootMode::${bootMode}".toString()
            }
        } else {
            updateSystemTag {
                uuid = existing.get(0).uuid
                tag = "bootMode::${bootMode}".toString()
            }
        }
    }

    void registerBaseStubs() {
        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus b ->
            b.reply(msg, new UploadImageToRemoteTargetReply())
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
