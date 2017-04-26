package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.LocalStorageSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.test.integration.storage.CephEnv
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.header.image.ImageConstant
import org.zstack.testlib.BackupStorageSpec
import org.zstack.utils.data.SizeUnit
import org.zstack.storage.ceph.backup.CephBackupStorageBase

/**
 * Created by SyZhao on 2017/4/17.
 */
class CephDetachAttachClusterCapacityCase extends SubCase {
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
            testDetachAttachClusterCapacity()
        }
    }

    void testDetachAttachClusterCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        ClusterInventory cluster = env.inventoryByName("test-cluster")
        ImageInventory image = env.inventoryByName("test-iso")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        def bs = env.inventoryByName("ceph-bk")
        def image_virtual_size = SizeUnit.GIGABYTE.toByte(10)//10G
        def image_physical_size = SizeUnit.GIGABYTE.toByte(1)//1G

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        def download_image_path_invoked = false
        env.simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            download_image_path_invoked = true
            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
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
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity + SizeUnit.GIGABYTE.toByte(20) + image_physical_size

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
        //env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
        //    LocalStorageSpec lspec = spec.specByUuid(ps.uuid)

        //    def rsp = new LocalStorageKvmBackend.AgentResponse()
        //    rsp.totalCapacity = lspec.totalCapacity
        //    rsp.availableCapacity = lspec.availableCapacity
        //    return rsp
        //}

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        GetPrimaryStorageCapacityResult capacityResult2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert capacityResult.availableCapacity == capacityResult2.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult capacityResult3 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert capacityResult.availableCapacity == capacityResult3.availableCapacity
        assert capacityResult.availablePhysicalCapacity == capacityResult3.availablePhysicalCapacity
    }
}
