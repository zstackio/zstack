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
        env = CephEnv.CephStorageOneVmEnv()
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
        def image_physical_size = SizeUnit.GIGABYTE.toByte(1)//1G

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

        createVmInstance {
            name = "crt-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }

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
