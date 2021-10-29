package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by SyZhao on 2017/4/17.
 */
class CloneCephCreateVmByImageCapacityCase extends SubCase {
    EnvSpec env

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
        env = Test.makeEnv {
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
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "test-vm"
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
            testCreateVmByImageCheckCapacity()
        }
    }

    void testCreateVmByImageCheckCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        ClusterInventory cluster = env.inventoryByName("test-cluster")
        ImageInventory image = env.inventoryByName("image")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        def bs = env.inventoryByName("ceph-bk")

        def download_image_path_invoked = false
        def image_virtual_size = SizeUnit.GIGABYTE.toByte(10)//10G
        def image_physical_size = SizeUnit.GIGABYTE.toByte(10)//10G

        env.simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            download_image_path_invoked = true
            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }

        assert download_image_path_invoked

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        } as VmInstanceInventory

        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity + image_virtual_size + image_physical_size

        //添加数据云盘
        def volume1 = createDataVolume {
            name = "dataVolume1"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory
        def volume2 = createDataVolume {
            name = "dataVolume2"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory
        attachDataVolumeToVm {
            volumeUuid = volume1.uuid
            vmInstanceUuid = vm.uuid
        }
        attachDataVolumeToVm {
            volumeUuid = volume2.uuid
            vmInstanceUuid = vm.uuid
        }

        cloneVmInstance {
            names = ["test"]
            vmInstanceUuid = vm.uuid
            full = true
        }

        assert 1 == 1
    }
}
