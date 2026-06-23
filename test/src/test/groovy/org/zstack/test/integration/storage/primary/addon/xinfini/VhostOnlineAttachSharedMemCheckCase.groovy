package org.zstack.test.integration.storage.primary.addon.xinfini

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.storage.backup.BackupStorageSystemTags
import org.zstack.tag.SystemTagCreator
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_VHOST_KVM_10002

class VhostOnlineAttachSharedMemCheckCase extends SubCase {

    EnvSpec env

    ClusterInventory cephCluster
    ClusterInventory vhostCluster
    InstanceOfferingInventory instanceOffering
    DiskOfferingInventory diskOffering
    ImageInventory image
    ImageInventory cephImage
    L3NetworkInventory l3
    PrimaryStorageInventory cephPs
    PrimaryStorageInventory vhostPs
    BackupStorageInventory bs
    HostInventory cephHost
    HostInventory vhostHost

    String xinfiniUrl = "http://127.0.0.1:8989"
    String xinfiniConfig = '{"token":"test-token","pools":[{"id":1,"name":"pool1"}],"nodes":[{"ip":"127.0.0.1","port":8989}]}'

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
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 2
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(10)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.4"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                    virtio = true
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-kvm"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachL2Network("l2")
                    attachPrimaryStorage("ceph-pri")
                }

                cluster {
                    name = "vhost-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "vhost-kvm"
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
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "ceph primary"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(500)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(500)
                    url = "ceph://pri"
                    fsid = "aabbccdd-1234-5678-9abc-def012345678"
                    monUrls = ["root:password@127.0.0.2/?monPort=7777"]
                }

                attachBackupStorage("sftp")
                attachBackupStorage("ceph-bs")
            }

            cephBackupStorage {
                name = "ceph-bs"
                description = "ceph backup storage"
                totalCapacity = SizeUnit.GIGABYTE.toByte(500)
                availableCapacity = SizeUnit.GIGABYTE.toByte(500)
                url = "/ceph-bk"
                fsid = "aabbccdd-1234-5678-9abc-def012345678"
                monUrls = ["root:password@127.0.0.2/?monPort=7777"]

                image {
                    name = "ceph-image"
                    url = "http://zstack.org/download/ceph-image.qcow2"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            cephCluster = env.inventoryByName("ceph-cluster") as ClusterInventory
            vhostCluster = env.inventoryByName("vhost-cluster") as ClusterInventory
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            cephImage = env.inventoryByName("ceph-image") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            cephPs = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
            bs = env.inventoryByName("sftp") as BackupStorageInventory
            cephHost = env.inventoryByName("ceph-kvm") as HostInventory
            vhostHost = env.inventoryByName("vhost-kvm") as HostInventory

            VmGlobalConfig.GENERATE_CONFIG_VHOST_REQUIRED.updateValue("false")

            setupVhostPs()
            setupAttachVolumeSimulator()
            testCephRootRunningVmBlocksVhostOnlineAttach()
            testCephRootStoppedVmAllowsVhostAttach()
            testVhostRootRunningVmAllowsVhostOnlineAttach()
        }
    }

    void setupVhostPs() {
        SystemTagCreator creator = BackupStorageSystemTags.ISCSI_INITIATOR_NAME.newSystemTagCreator(bs.uuid)
        creator.setTagByTokens([(BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN): "iqn.1994-05.com.redhat:fc16b4d4fb3f"])
        creator.inherent = false
        creator.recreate = true
        creator.create()

        vhostPs = addExternalPrimaryStorage {
            name = "xinfini-vhost"
            zoneUuid = (env.inventoryByName("zone") as ZoneInventory).uuid
            url = xinfiniUrl
            identity = "xinfini"
            config = xinfiniConfig
            defaultOutputProtocol = "Vhost"
        } as ExternalPrimaryStorageInventory

        attachPrimaryStorageToCluster {
            primaryStorageUuid = vhostPs.uuid
            clusterUuid = vhostCluster.uuid
        }
        attachPrimaryStorageToCluster {
            primaryStorageUuid = vhostPs.uuid
            clusterUuid = cephCluster.uuid
        }
    }

    void setupAttachVolumeSimulator() {
        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus bus ->
            bus.reply(msg, new UploadImageToRemoteTargetReply())
        }

        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { KVMAgentCommands.AttachDataVolumeResponse rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachDataVolumeCmd.class)
            rsp.virtualDeviceInfoList = []
            return rsp
        }
    }

    void testCephRootRunningVmBlocksVhostOnlineAttach() {
        VmInstanceInventory cephVm = createVmInstance {
            name = "ceph-root-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = cephImage.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = cephHost.uuid
        } as VmInstanceInventory

        assert VmInstanceState.Running.toString() == cephVm.state

        VolumeInventory vhostVol = createDataVolume {
            name = "vhost-data-vol"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = vhostPs.uuid
        } as VolumeInventory

        boolean kvmagentCalled = false
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            kvmagentCalled = true
            return rsp
        }

        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction()
        action.vmInstanceUuid = cephVm.uuid
        action.volumeUuid = vhostVol.uuid
        action.sessionId = adminSession()
        AttachDataVolumeToVmAction.Result ret = action.call()

        assert ret.error != null : "online attach of vhost volume to a no-shared-mem running VM must be rejected"
        assert JSONObjectUtil.toJsonString(ret.error).contains(ORG_ZSTACK_VHOST_KVM_10002) :
                "expected guard error ORG_ZSTACK_VHOST_KVM_10002 in the error chain, got: ${JSONObjectUtil.toJsonString(ret.error)}"
        assert !kvmagentCalled : "kvmagent must not be called when MN pre-attach guard rejects"

        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { KVMAgentCommands.AttachDataVolumeResponse rsp, HttpEntity<String> e ->
            rsp.virtualDeviceInfoList = []
            return rsp
        }

        destroyVmInstance { uuid = cephVm.uuid }
        expungeVmInstance { uuid = cephVm.uuid }
        deleteDataVolume { uuid = vhostVol.uuid }
        expungeDataVolume { uuid = vhostVol.uuid }
    }

    void testCephRootStoppedVmAllowsVhostAttach() {
        VmInstanceInventory cephVm = createVmInstance {
            name = "ceph-root-vm-stopped"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = cephImage.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = cephHost.uuid
        } as VmInstanceInventory

        stopVmInstance { uuid = cephVm.uuid }
        cephVm = queryVmInstance { conditions = ["uuid=${cephVm.uuid}"] }[0] as VmInstanceInventory
        assert VmInstanceState.Stopped.toString() == cephVm.state

        VolumeInventory vhostVol = createDataVolume {
            name = "vhost-data-vol-offline"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = vhostPs.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = cephVm.uuid
            volumeUuid = vhostVol.uuid
        }

        detachDataVolumeFromVm { uuid = vhostVol.uuid }
        destroyVmInstance { uuid = cephVm.uuid }
        expungeVmInstance { uuid = cephVm.uuid }
        deleteDataVolume { uuid = vhostVol.uuid }
        expungeDataVolume { uuid = vhostVol.uuid }
    }

    void testVhostRootRunningVmAllowsVhostOnlineAttach() {
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            return rsp
        }

        VmInstanceInventory vhostVm = createVmInstance {
            name = "vhost-root-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = vhostHost.uuid
            rootVolumeSystemTags = []
        } as VmInstanceInventory

        assert VmInstanceState.Running.toString() == vhostVm.state

        SQL.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, vhostVm.rootVolumeUuid)
                .set(VolumeVO_.protocol, "Vhost")
                .update()

        VolumeInventory vhostVol2 = createDataVolume {
            name = "vhost-data-vol2"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = vhostPs.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vhostVm.uuid
            volumeUuid = vhostVol2.uuid
        }

        detachDataVolumeFromVm { uuid = vhostVol2.uuid }
        destroyVmInstance { uuid = vhostVm.uuid }
        expungeVmInstance { uuid = vhostVm.uuid }
        deleteDataVolume { uuid = vhostVol2.uuid }
        expungeDataVolume { uuid = vhostVol2.uuid }
    }
}
