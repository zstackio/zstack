
package org.zstack.test.integration.storage.backup.ceph.imagebackupstoragerefvo

import org.zstack.core.db.Q
import org.zstack.header.image.*
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/3/31.
 */
class AddImageCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone{
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    attachPrimaryStorage("ceph-pri")
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
        }
    }

    @Override
    void test() {
        env.create {
            testImageBackupStorageRefVOWhenAddImage()
            testUploadImage()
        }
    }

    void testImageBackupStorageRefVOWhenAddImage(){
        BackupStorageInventory bs = env.inventoryByName("ceph-bk")

        ImageInventory newImage = addImage {
            name = "large-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        ImageInventory newImage1 = addImage {
            name = "image2"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        ImageInventory newImage2 = addImage {
            name = "image3"
            url = "file:///my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        ImageInventory newImage3 = addImage {
            name = "image4"
            url = "/my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage1.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage2.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage3.uuid)
                .count()
    }

    void testUploadImage(){
        def originSize = 0
        def updatedSize = 2048
        env.simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = originSize
            rsp.uploadPath = "http://localhost:7071/ceph/image/upload"
            return rsp
        }

        env.simulator(CephBackupStorageBase.GET_DOWNLOAD_PROGRESS_PATH) {
            def rsp = new CephBackupStorageBase.GetDownloadProgressRsp()
            rsp.completed = true
            rsp.size = updatedSize
            rsp.installPath = "dummy-pool/dummy-image"
            return rsp
        }

        BackupStorageInventory bs = env.inventoryByName("ceph-bk")
        ImageInventory inv = addImage {
            name = "test-upload"
            url = "upload://myimage.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        assert inv != null
        assert inv.status == ImageStatus.Downloading.toString()
        assert inv.size == originSize

        retryInSecs {
            ImageVO image = dbFindByUuid(inv.uuid, ImageVO.class)
            assert image != null
            assert image.size == updatedSize
            assert image.status == ImageStatus.Ready
        }

        BackupStorageInventory bs_now = env.inventoryByName("ceph-bk")
        assert bs.totalCapacity == bs_now.totalCapacity
        assert bs.availableCapacity == bs_now.availableCapacity + inv.actualSize
    }

    @Override
    void clean() {
        env.delete()
    }
}