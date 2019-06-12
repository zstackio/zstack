package org.zstack.test.integration.core.trash

import org.zstack.core.Platform
import org.zstack.core.db.UpdateQuery
import org.zstack.core.jsonlabel.JsonLabelInventory
import org.zstack.core.trash.StorageTrashImpl
import org.zstack.core.trash.TrashType
import org.zstack.header.storage.backup.StorageTrashSpec
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.CleanUpTrashOnPrimaryStorageAction
import org.zstack.sdk.CleanUpTrashOnPrimaryStorageResult
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetTrashOnPrimaryStorageResult
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2018/12/18.*/
class PrimaryStorageTrashCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    DiskOfferingInventory diskOffering
    StorageTrashImpl trashMrg
    JsonLabelInventory label

    VolumeInventory volume
    VolumeSnapshotInventory snapshot

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
            createTrashSnapshot()
            volume = createDataVolume {
                name = "volume"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            } as VolumeInventory

            snapshot = createVolumeSnapshot {
                name = "snapshot"
                volumeUuid = volume.uuid
            } as VolumeSnapshotInventory
            testCleanUpTrashWhileUsing()
        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-ps") as PrimaryStorageInventory
        diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        trashMrg = bean(StorageTrashImpl.class)
    }

    void createTrash() {
        trashResourceUuid = Platform.uuid
        def spec = new StorageTrashSpec(trashResourceUuid, VolumeVO.class.getSimpleName(), ps.uuid, PrimaryStorageVO.class.getSimpleName(),
                "mock-installpath-${trashResourceUuid}", 123456L)
        label = trashMrg.createTrash(TrashType.MigrateVolume, spec) as JsonLabelInventory

        assert label.resourceUuid == ps.uuid
        assert label.labelKey.startsWith(TrashType.MigrateVolume.toString())
        def s = JSONObjectUtil.toObject(label.labelValue, StorageTrashSpec.class)
        assert s.size == 123456L
        assert s.installPath == "mock-installpath-${trashResourceUuid}"
        assert s.resourceUuid == trashResourceUuid
        assert s.storageUuid == ps.uuid
        assert s.storageType == "PrimaryStorageVO"
        assert s.resourceType == "VolumeVO"
    }

    void createTrashSnapshot() {
        String resourceUuid = Platform.uuid
        def spec = new StorageTrashSpec(resourceUuid, VolumeSnapshotVO.class.getSimpleName(), ps.uuid, PrimaryStorageVO.class.getSimpleName(),
                "mock-installpath-${resourceUuid}", 123456L)
        label = trashMrg.createTrash(TrashType.MigrateVolumeSnapshot, spec) as JsonLabelInventory

        assert label.resourceUuid == ps.uuid
        assert label.labelKey.startsWith(TrashType.MigrateVolume.toString())
        def s = JSONObjectUtil.toObject(label.labelValue, StorageTrashSpec.class)
        assert s.size == 123456L
        assert s.installPath == "mock-installpath-${resourceUuid}"
        assert s.resourceUuid == resourceUuid
        assert s.storageUuid == ps.uuid
        assert s.storageType == "PrimaryStorageVO"
        assert s.resourceType == "VolumeSnapshotVO"
    }

    void testGetTrash() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult
        assertTrash(trashs)

        trashs = getTrashOnPrimaryStorage {
            delegate.uuid = ps.uuid
            delegate.resourceType = "VolumeVO"
            delegate.resourceUuid = trashResourceUuid
            delegate.trashType = "MigrateVolume"
        } as GetTrashOnPrimaryStorageResult

        assertTrash(trashs)
    }

    void assertTrash(GetTrashOnPrimaryStorageResult trashs) {
        assert trashs.storageTrashSpecs.get(0).size == 123456L
        assert trashs.storageTrashSpecs.get(0).installPath.startsWith("mock-installpath")
        assert trashs.storageTrashSpecs.get(0).storageUuid == ps.uuid
        assert trashs.storageTrashSpecs.get(0).storageType == "PrimaryStorageVO"
        assert trashs.storageTrashSpecs.get(0).resourceType == "VolumeVO"
        assert trashs.storageTrashSpecs.get(0).trashType == "MigrateVolume"
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

    // if installpath still in using (this situation usually caused by bug), then skip delete it
    void testCleanUpTrashWhileUsing() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult

        assert trashs.storageTrashSpecs.size() > 0
        def count = trashs.storageTrashSpecs.size()

        trashs.storageTrashSpecs.each {
            def trash = it as org.zstack.sdk.StorageTrashSpec
            if (trash.resourceType == "VolumeVO") {
                UpdateQuery.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).set(VolumeVO_.installPath, trash.installPath).update()
            } else if (trash.resourceType == "VolumeSnapshotVO") {
                UpdateQuery.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).set(VolumeSnapshotVO_.primaryStorageInstallPath, trash.installPath).update()
            }
        }

        def action = new CleanUpTrashOnPrimaryStorageAction()
        action.uuid = ps.uuid
        action.sessionId = adminSession()

        def result = action.call()

        assert result.error != null

        trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as GetTrashOnPrimaryStorageResult

        assert trashs.storageTrashSpecs.size() == count
    }
}
