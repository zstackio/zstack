package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.*
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase

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
        env = makeEnv {
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
            }

            zone {
                name = "zone"
                description = "test zone"

                attachBackupStorage("sftp")
            }

        }
    }

    @Override
    void test() {
        env.create {
            testDeleteImage()
            testDeleteDownloadingImage()
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

        // use '.eq(ImageVO_.name, ...)' will result to 'ImageVO_.getName()'
        def image = Q.New(ImageVO.class).list().find { it.name == imageName }
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
}
