package org.zstack.test.integration.core.trash

import org.zstack.core.db.Q
import org.zstack.core.trash.StorageRecycleImpl
import org.zstack.core.trash.TrashType
import org.zstack.header.core.trash.InstallPathRecycleInventory
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CleanUpTrashOnBackupStorageAction
import org.zstack.sdk.CleanUpTrashOnBackupStorageResult
import org.zstack.sdk.ImageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2018/12/18.*/
class BackupStorageTrashCase extends SubCase {
    EnvSpec env
    BackupStorageInventory bs
    ImageInventory image
    StorageRecycleImpl trashMrg
    InstallPathRecycleInventory label

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
        env = TrashEnv.psbs()
    }

    @Override
    void test() {
        env.create {
            prepare()
            createTrash()
            testGetTrash()
            testCleanUpTrashWhileUsing()

            createTrash()
            createTrash()

            deleteImage {
                uuid = image.uuid
            }
            expungeImage {
                imageUuid = image.uuid
            }
            testDeleteTrashSingle()
            testCleanUpTrash()
        }
    }

    void prepare() {
        bs = env.inventoryByName("ceph-bs") as BackupStorageInventory
        trashMrg = bean(StorageRecycleImpl.class)

        image = addImage {
            name = "image-for-test"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        } as ImageInventory
    }

    void createTrash() {
        def vo = Q.New(ImageVO.class).eq(ImageVO_.uuid, image.uuid).find() as ImageVO
        def i = org.zstack.header.image.ImageInventory.valueOf(vo)
        i.url = image.backupStorageRefs.get(0).installPath
        i.description = bs.uuid
        label = trashMrg.createTrash(TrashType.MigrateImage, false, i) as InstallPathRecycleInventory

        assert label.resourceUuid == image.uuid
        assert label.storageUuid == bs.uuid
        assert label.storageType == "BackupStorageVO"
        assert label.resourceType == "ImageVO"
        assert label.trashType == TrashType.MigrateImage.toString()
        assert label.size == 0L
        assert label.installPath == i.url
    }

    void testGetTrash() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assertTrash(trashs)

        trashs = getTrashOnBackupStorage {
            delegate.uuid = bs.uuid
            delegate.resourceType = "ImageVO"
            delegate.resourceUuid = image.uuid
            delegate.trashType = "MigrateImage"
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assertTrash(trashs)
    }

    void assertTrash(List<org.zstack.sdk.InstallPathRecycleInventory> trashs) {
        assert trashs.get(0).size == 0L
        assert trashs.get(0).installPath == image.backupStorageRefs.get(0).installPath
        assert trashs.get(0).storageUuid == bs.uuid
        assert trashs.get(0).storageType == "BackupStorageVO"
        assert trashs.get(0).resourceType == "ImageVO"
        assert trashs.get(0).resourceUuid == image.uuid
        assert trashs.get(0).trashType == "MigrateImage"
        assert trashs.get(0).trashId > 0
    }

    void testDeleteTrashSingle() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>
        def count = trashs.size()

        cleanUpTrashOnBackupStorage {
            uuid = bs.uuid
            trashId = label.trashId
        }

        trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() == count - 1
    }

    void testCleanUpTrash() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>
        def count = trashs.size()

        def result = cleanUpTrashOnBackupStorage {
            uuid = bs.uuid
        } as CleanUpTrashOnBackupStorageResult

        assert result.result != null
        assert result.result.size == image.size * count
        assert result.result.resourceUuids.size() == count

        trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() == 0
    }

    void testCleanUpTrashWhileUsing() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>
        def count = trashs.size()

        def action = new CleanUpTrashOnBackupStorageAction()
        action.uuid = bs.uuid
        action.trashId = label.trashId
        action.sessionId = adminSession()

        def result = action.call()
        assert result.error == null
        assert result.value.result.details != null

        trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() == count
    }
}
