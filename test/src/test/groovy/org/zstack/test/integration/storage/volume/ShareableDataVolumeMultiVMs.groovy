package org.zstack.test.integration.storage.volume

import junit.framework.Assert
import org.zstack.header.message.MessageReply
import org.zstack.header.volume.APICreateDataVolumeMsg
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-3-24.
 */
class ShareableDataVolumeMultiVMs extends SubCase{

    EnvSpec env
    VolumeInventory voi

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }
            zone{
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-mon"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    kvm {
                        name = "host"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ceph-pri")
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

                cephPrimaryStorage {
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }


                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm1"
                useCluster("test-cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }

            vm {
                name = "vm2"
                useCluster("test-cluster")
                useHost("host")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")

            }



        }
    }

    @Override
    void test() {
        env.create {
            createShareableDataVolume()
            AttachSharebleDataVolumeToVms()
        }
    }

    void createShareableDataVolume(){
        String tag = VolumeSystemTags.SHAREABLE.getTagFormat();
        String tag2 = KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat();
        DiskOfferingInventory disk = env.inventoryByName("diskOffering")

        voi = createDataVolume {
            name = "share"
            diskOfferingUuid = disk.uuid
            systemTags = Arrays.asList(tag,tag2)
        }
    }
    void AttachSharebleDataVolumeToVms(){
        VmInstanceInventory vmi1 = env.inventoryByName("vm1")
        VmInstanceInventory vmi2 = env.inventoryByName("vm2")

        attachDataVolumeToVm {
            volumeUuid = voi.uuid
            vmInstanceUuid = vmi1.uuid
        }

        attachDataVolumeToVm {
            volumeUuid = voi.uuid
            vmInstanceUuid = vmi2.uuid
        }
    }
    @Override
    void clean() {
        env.delete()
    }
}