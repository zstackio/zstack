package org.zstack.test.integration.core.trash

import org.zstack.core.Platform
import org.zstack.core.jsonlabel.JsonLabelInventory
import org.zstack.core.trash.StorageTrashImpl
import org.zstack.core.trash.TrashType
import org.zstack.header.image.ImageVO
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.StorageTrashSpec
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.GetTrashOnBackupStorageResult
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2018/12/18.*/
class BackupStorageTrashCase extends SubCase {
    EnvSpec env
    BackupStorageInventory bs
    StorageTrashImpl trashMrg
    JsonLabelInventory label

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
        }
    }

    void prepare() {
        bs = env.inventoryByName("ceph-bs") as BackupStorageInventory
        trashMrg = bean(StorageTrashImpl.class)
    }

    void createTrash() {
        String resourceUuid = Platform.uuid
        def spec = new StorageTrashSpec(resourceUuid, ImageVO.class.getSimpleName(), bs.uuid, BackupStorageVO.class.getSimpleName(),
                "mock-installpath", 123456L)
        label = trashMrg.createTrash(TrashType.MigrateImage, spec) as JsonLabelInventory

        assert label.resourceUuid == bs.uuid
        assert label.labelKey.startsWith(TrashType.MigrateImage.toString())
        def s = JSONObjectUtil.toObject(label.labelValue, StorageTrashSpec.class)
        assert s.size == 123456L
        assert s.installPath == "mock-installpath"
        assert s.resourceUuid == resourceUuid
        assert s.storageUuid == bs.uuid
        assert s.storageType == "BackupStorageVO"
        assert s.resourceType == "ImageVO"
    }

    void testGetTrash() {
        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult

        assert trashs.storageTrashSpecs.get(0).size == 123456L
        assert trashs.storageTrashSpecs.get(0).installPath == "mock-installpath"
        assert trashs.storageTrashSpecs.get(0).storageUuid == bs.uuid
        assert trashs.storageTrashSpecs.get(0).storageType == "BackupStorageVO"
        assert trashs.storageTrashSpecs.get(0).resourceType == "ImageVO"
        assert trashs.storageTrashSpecs.get(0).id > 0
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
        cleanUpTrashOnBackupStorage {
            uuid = bs.uuid
        }

        def trashs = getTrashOnBackupStorage {
            uuid = bs.uuid
        } as GetTrashOnBackupStorageResult

        assert trashs.storageTrashSpecs.size() == 0
    }
}
