package org.zstack.test.integration.image

import org.zstack.header.image.ImageStatus
import org.zstack.header.image.ImageVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase

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
}
