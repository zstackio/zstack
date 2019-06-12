package org.zstack.test.integration.core.trash

import org.zstack.core.Platform
import org.zstack.core.db.UpdateQuery
import org.zstack.core.jsonlabel.JsonLabelInventory
import org.zstack.core.trash.StorageTrashImpl
import org.zstack.core.trash.TrashType
import org.zstack.header.image.ImageBackupStorageRefVO
import org.zstack.header.image.ImageBackupStorageRefVO_
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageVO
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.StorageTrashSpec
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CleanUpTrashOnBackupStorageAction
import org.zstack.sdk.CleanUpTrashOnBackupStorageResult
import org.zstack.sdk.GetTrashOnBackupStorageResult
import org.zstack.sdk.ImageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2018/12/18.*/
class BackupStorageTrashCase extends SubCase {
    EnvSpec env
    BackupStorageInventory bs
    ImageInventory image
    StorageTrashImpl trashMrg
    JsonLabelInventory label

    String trashResourceUuid

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

            createTrash()
            createTrash()
            testDeleteTrashSingle()
            testCleanUpTrash()

            createTrash()
            image = addImage {
                name = "image-for-test"
                url = "http://my-site/foo.qcow2"
                backupStorageUuids = [bs.uuid]
                format = ImageConstant.QCOW2_FORMAT_STRING
            } as ImageInventory
            testCleanUpTrashWhileUsing()
        }
    }

    void prepare() {
        bs = env.inventoryByName("ceph-bs") as BackupStorageInventory
        trashMrg = bean(StorageTrashImpl.class)
    }

    void createTrash() {
        trashResourceUuid = Platform.uuid
        def spec = new StorageTrashSpec(trashResourceUuid, ImageVO.class.getSimpleName(), bs.uuid, BackupStorageVO.class.getSimpleName(),
                "mock-installpath", 123456L)
        label = trashMrg.createTrash(TrashType.MigrateImage, spec) as JsonLabelInventory

        assert label.resourceUuid == bs.uuid
        assert label.labelKey.startsWith(TrashType.MigrateImage.toString())
        def s = JSONObjectUtil.toObject(label.labelValue, StorageTrashSpec.class)
        assert s.size == 123456L
        assert s.installPath == "mock-installpath"
        assert s.resourceUuid == trashResourceUuid
        assert s.storageUuid == bs.uuid
        assert s.storageType == "BackupStorageVO"
        assert s.resourceType == "ImageVO"
    }

    void testGetTrash() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult

        assertTrash(trashs)

        trashs = getTrashOnBackupStorage {
            delegate.uuid = bs.uuid
            delegate.resourceType = "ImageVO"
            delegate.resourceUuid = trashResourceUuid
            delegate.trashType = "MigrateImage"
        } as GetTrashOnBackupStorageResult

        assertTrash(trashs)
    }

    void assertTrash(GetTrashOnBackupStorageResult trashs) {
        assert trashs.storageTrashSpecs.get(0).size == 123456L
        assert trashs.storageTrashSpecs.get(0).installPath == "mock-installpath"
        assert trashs.storageTrashSpecs.get(0).storageUuid == bs.uuid
        assert trashs.storageTrashSpecs.get(0).storageType == "BackupStorageVO"
        assert trashs.storageTrashSpecs.get(0).resourceType == "ImageVO"
        assert trashs.storageTrashSpecs.get(0).resourceUuid == trashResourceUuid
        assert trashs.storageTrashSpecs.get(0).trashType == "MigrateImage"
        assert trashs.storageTrashSpecs.get(0).trashId > 0
        assert trashs.storageTrashSpecs.get(0).createDate == label.createDate
    }

    void testDeleteTrashSingle() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult
        def count = trashs.storageTrashSpecs.size()

        cleanUpTrashOnBackupStorage {
            uuid = bs.uuid
            trashId = label.id
        }

        trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult

        assert trashs.storageTrashSpecs.size() == count - 1
    }

    void testCleanUpTrash() {
        def result = cleanUpTrashOnBackupStorage {
            uuid = bs.uuid
        } as CleanUpTrashOnBackupStorageResult

        assert result.result != null
        assert result.result.size == 246912L
        assert result.result.resourceUuids.size() == 2

        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult

        assert trashs.storageTrashSpecs.size() == 0
    }

    void testCleanUpTrashWhileUsing() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult
        def count = trashs.storageTrashSpecs.size()

        trashs.storageTrashSpecs.each {
            def trash = it as org.zstack.sdk.StorageTrashSpec
            if (trash.resourceType == "ImageVO") {
                UpdateQuery.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, image.uuid).
                        set(ImageBackupStorageRefVO_.installPath, trash.installPath).update()
            }
        }

        def action = new CleanUpTrashOnBackupStorageAction()
        action.uuid = bs.uuid
        action.trashId = label.id
        action.sessionId = adminSession()

        def result = action.call()
        assert result.error != null

        trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult

        assert trashs.storageTrashSpecs.size() == count
    }
}
