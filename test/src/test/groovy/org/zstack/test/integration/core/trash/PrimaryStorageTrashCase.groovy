package org.zstack.test.integration.core.trash

import org.zstack.core.Platform
import org.zstack.core.jsonlabel.JsonLabelInventory
import org.zstack.core.trash.StorageTrashImpl
import org.zstack.core.trash.TrashType
import org.zstack.header.storage.backup.StorageTrashSpec
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.volume.VolumeVO
import org.zstack.sdk.CleanUpTrashOnPrimaryStorageResult
import org.zstack.sdk.GetTrashOnPrimaryStorageResult
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2018/12/18.*/
class PrimaryStorageTrashCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
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
        ps = env.inventoryByName("ceph-ps") as PrimaryStorageInventory
        trashMrg = bean(StorageTrashImpl.class)
    }

    void createTrash() {
        String resourceUuid = Platform.uuid
        def spec = new StorageTrashSpec(resourceUuid, VolumeVO.class.getSimpleName(), ps.uuid, PrimaryStorageVO.class.getSimpleName(),
                "mock-installpath", 123456L)
        label = trashMrg.createTrash(TrashType.MigrateVolume, spec) as JsonLabelInventory

        assert label.resourceUuid == ps.uuid
        assert label.labelKey.startsWith(TrashType.MigrateVolume.toString())
        def s = JSONObjectUtil.toObject(label.labelValue, StorageTrashSpec.class)
        assert s.size == 123456L
        assert s.installPath == "mock-installpath"
        assert s.resourceUuid == resourceUuid
        assert s.storageUuid == ps.uuid
        assert s.storageType == "PrimaryStorageVO"
        assert s.resourceType == "VolumeVO"
    }

    void testGetTrash() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult

        assert trashs.storageTrashSpecs.get(0).size == 123456L
        assert trashs.storageTrashSpecs.get(0).installPath == "mock-installpath"
        assert trashs.storageTrashSpecs.get(0).storageUuid == ps.uuid
        assert trashs.storageTrashSpecs.get(0).storageType == "PrimaryStorageVO"
        assert trashs.storageTrashSpecs.get(0).resourceType == "VolumeVO"
        assert trashs.storageTrashSpecs.get(0).trashId > 0
        assert trashs.storageTrashSpecs.get(0).createDate == label.createDate
    }

    void testDeleteTrashSingle() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult
        def count = trashs.storageTrashSpecs.size()

        cleanUpTrashOnPrimaryStorage {
            uuid = ps.uuid
            trashId = label.id
        }

        trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult

        assert trashs.storageTrashSpecs.size() == count - 1
    }

    void testCleanUpTrash() {
        def result = cleanUpTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as CleanUpTrashOnPrimaryStorageResult

        assert result.result != null
        assert result.result.size == 246912L
        assert result.result.resourceUuids.size() == 2

        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult

        assert trashs.storageTrashSpecs.size() == 0
    }

}
