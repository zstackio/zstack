package org.zstack.test.integration.storage.backup.ceph.imagebackupstoragerefvo

import org.zstack.core.db.Q
import org.zstack.header.image.ImageBackupStorageRefVO
import org.zstack.header.image.ImageBackupStorageRefVO_
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
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
            url = "ceph://bak-t-9b278a2c62fb4ed281b1695a6ebdbc8c/335b922a93cd4c529a08c63e431f2345"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage.uuid)
                .count()
        assert 1 == Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, newImage1.uuid)
                .count()
    }
    
    @Override
    void clean() {
        env.delete()
    }
}