package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.zstack.core.gc.GCStatus
import org.zstack.header.image.ImageVO
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.ImageInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/5.
 */
class ImageGCCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ImageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneSftpEnv
    }

    void testImageGCWhenBackupStorageDisconnect() {
        ImageInventory image = env.inventoryByName("image")

        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions=["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv != null
        assert inv.status == GCStatus.Idle.toString()

        SftpBackupStorageCommands.DeleteCmd cmd = null

        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, SftpBackupStorageCommands.DeleteCmd.class)
            return rsp
        }

        triggerGCJob {
            uuid = inv.uuid
        }

        TimeUnit.SECONDS.sleep(2)

        assert cmd != null
        assert !dbIsExists(image.uuid, ImageVO.class)

        inv = queryGCJob {
            conditions=["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv.status == GCStatus.Done.toString()
    }

    void testImageGCCancelledAfterBackupStorageDeleted() {
        ImageInventory image = env.inventoryByName("image")

        env.afterSimulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions=["context~=%${image.uuid}%".toString()]
        }[0]

        assert inv != null
        assert inv.status == GCStatus.Idle.toString()

        deleteGCJob {
            uuid = inv.uuid
        }

        assert (queryGCJob {
            conditions=["context~=%${image.uuid}%".toString()]
        } as List).isEmpty()
    }

    @Override
    void test() {
        env.create {
            // make the interval very long, we use api to trigger the job to test
            ImageGlobalConfig.DELETION_GARBAGE_COLLECTION_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))

            testImageGCWhenBackupStorageDisconnect()

            env.recreate("image")

            testImageGCCancelledAfterBackupStorageDeleted()
        }
    }
}
