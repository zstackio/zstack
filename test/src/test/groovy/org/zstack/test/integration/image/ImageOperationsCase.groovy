package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.*
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import java.util.concurrent.TimeUnit

/**
 * Created by david on 3/2/17.
 */
class ImageOperationsCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(ImageTest.springSpec)
    }

    @Override
    void environment() {
       env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "username"
                password = "password"
                hostname = "hostname"

                image {
                    name = "image"
                    url = "http://somehost/boot.iso"
                    format = "iso"
                }

                image {
                    name = "image1"
                    url = "http://somehost/boot.iso"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
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
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteImage()
            testDeleteImageWhichUsedInVm()
            testDeleteDownloadingImage()
            testImageBackupStorageRefVOHasInfoWhenImageDownloading()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testDeleteImage() {
        def thisImageUuid = (env.specByName("image") as ImageSpec).inventory.uuid
        deleteImage {
            uuid = thisImageUuid
        }

        ImageVO vo = dbFindByUuid(thisImageUuid, ImageVO.class)
        assert vo.status == ImageStatus.Deleted
        assert vo.backupStorageRefs.size() == 1
        assert vo.backupStorageRefs[0].status == vo.status

        recoverImage {
            imageUuid = thisImageUuid
        }

        vo = dbFindByUuid(thisImageUuid, ImageVO.class)
        assert vo.status == ImageStatus.Ready
        assert vo.backupStorageRefs.size() == 1
        assert vo.backupStorageRefs[0].status == vo.status

        deleteImage {
            uuid = thisImageUuid
        }
        expungeImage {
            imageUuid = thisImageUuid
        }
    }

    void testDeleteImageWhichUsedInVm(){
        org.zstack.sdk.ImageInventory usedImage = env.inventoryByName("image1")
        deleteImage {
            uuid = usedImage.uuid
        }
        expungeImage {
            imageUuid = usedImage.uuid
        }
    }

    void testDeleteDownloadingImage() {
        def bs = env.inventoryByName("sftp")

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            rsp, HttpEntity<String> e ->
                TimeUnit.SECONDS.sleep(3)
                return rsp
        }

        def imageName = "large-image"
        def thread = Thread.start {
            addImage {
                name = imageName
                url = "http://my-site/foo.iso"
                backupStorageUuids = [bs.uuid]
                format = ImageConstant.ISO_FORMAT_STRING
            }
        }

        TimeUnit.SECONDS.sleep(1)

        ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
        assert image != null

        deleteImage {
            uuid = image.uuid
        }

        env.cleanSimulatorHandlers()
        assert !dbIsExists(image.uuid, ImageVO.class)

        thread.join()

        Long cnt = Q.New(ImageBackupStorageRefVO.class)
        .eq(ImageBackupStorageRefVO_.imageUuid, image.uuid)
        .count()

        assert cnt == 0L
    }

    void testImageBackupStorageRefVOHasInfoWhenImageDownloading() {
        def bs = env.inventoryByName("sftp")

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            rsp, HttpEntity<String> e ->
                TimeUnit.SECONDS.sleep(3)
                return rsp
        }

        def imageName = "large-image"
        def thread = Thread.start {
            addImage {
                name = imageName
                url = "http://my-site/foo.iso"
                backupStorageUuids = [bs.uuid]
                format = ImageConstant.ISO_FORMAT_STRING
            }
        }

        TimeUnit.SECONDS.sleep(1)
        ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.name, imageName).find()
        assert image != null

        env.cleanSimulatorHandlers()
        ImageBackupStorageRefVO vo = Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid,image.uuid).eq(ImageBackupStorageRefVO_.backupStorageUuid,bs.uuid).find()
        assert vo != null
        assert vo.status == ImageStatus.Downloading
    }
}
