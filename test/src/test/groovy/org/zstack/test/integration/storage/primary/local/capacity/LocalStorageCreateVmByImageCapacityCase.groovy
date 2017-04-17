package org.zstack.test.integration.storage.primary.local.capacity

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
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.utils.data.SizeUnit
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.testlib.BackupStorageSpec
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.header.image.ImageConstant
import org.zstack.core.db.Q


/**
 * Created by SyZhao on 2017/4/17.
 */
class LocalStorageCreateVmByImageCapacityCase extends SubCase {
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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmByImageCheckCapacity()
        }
    }

    void testCreateVmByImageCheckCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image1")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        def bs = env.inventoryByName("sftp")

        def download_image_path_invoked = false
        def image_virtual_size = SizeUnit.GIGABYTE.toByte(10)//10G
        def image_physical_size = SizeUnit.GIGABYTE.toByte(1)//1G
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
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

        def vm = createVmInstance {
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

        boolean checked = false
        env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            LocalStorageKvmBackend.InitCmd cmd = JSONObjectUtil.toObject(e.body,LocalStorageKvmBackend.InitCmd.class)

            LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                    .eq(LocalStorageHostRefVO_.hostUuid, cmd.hostUuid).find()

            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = refVO.totalPhysicalCapacity
            if(cmd.hostUuid == vm.hostUuid){
                rsp.availableCapacity = refVO.totalPhysicalCapacity - image_physical_size
            }else{
                rsp.availableCapacity = refVO.availablePhysicalCapacity
            }
            checked = true
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        assert checked

        GetPrimaryStorageCapacityResult afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity + image_physical_size
    }
}
