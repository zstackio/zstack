package org.zstack.test.integration.core.trash

import org.zstack.core.db.Q
import org.zstack.core.trash.StorageRecycleImpl
import org.zstack.core.trash.TrashType
import org.zstack.header.core.trash.InstallPathRecycleInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.CleanUpTrashOnPrimaryStorageAction
import org.zstack.sdk.CleanUpTrashOnPrimaryStorageResult
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2018/12/18.*/
class PrimaryStorageTrashCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    DiskOfferingInventory diskOffering
    StorageRecycleImpl trashMrg
    InstallPathRecycleInventory label

    VolumeInventory volume
    VolumeSnapshotInventory snapshot

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
            createTrashSnapshot()
            testCleanUpTrashWhileUsing()

            deleteDataVolume {
                uuid = volume.uuid
            }
            expungeDataVolume {
                uuid = volume.uuid
            }
            testDeleteTrashSingle()
            testCleanUpTrash()


        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-ps") as PrimaryStorageInventory
        diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        trashMrg = bean(StorageRecycleImpl.class)

        volume = createDataVolume {
            name = "volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        snapshot = createVolumeSnapshot {
            name = "snapshot"
            volumeUuid = volume.uuid
        } as VolumeSnapshotInventory
    }

    void createTrash() {
        def vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).find() as VolumeVO
        def v = org.zstack.header.volume.VolumeInventory.valueOf(vo)
        label = trashMrg.createTrash(TrashType.MigrateVolume, true, v) as InstallPathRecycleInventory

        assert label.size == volume.size
        assert label.installPath == volume.installPath
        assert label.resourceUuid == volume.uuid
        assert label.storageUuid == ps.uuid
        assert label.storageType == "PrimaryStorageVO"
        assert label.resourceType == "VolumeVO"
        assert label.trashType == TrashType.MigrateVolume.toString()
        assert label.folder
    }

    void createTrashSnapshot() {
        def vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).find() as VolumeSnapshotVO
        def s = org.zstack.header.storage.snapshot.VolumeSnapshotInventory.valueOf(vo)
        label = trashMrg.createTrash(TrashType.MigrateVolumeSnapshot, false, s) as InstallPathRecycleInventory

        assert label.resourceUuid == snapshot.uuid
        assert label.storageUuid == ps.uuid
        assert label.storageType == "PrimaryStorageVO"
        assert label.resourceType == "VolumeSnapshotVO"
        assert label.trashType == TrashType.MigrateVolumeSnapshot.toString()
        assert label.size == snapshot.size
        assert label.installPath == snapshot.primaryStorageInstallPath
    }

    void testGetTrash() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>
        assertTrash(trashs)

        trashs = getTrashOnPrimaryStorage {
            delegate.uuid = ps.uuid
            delegate.resourceType = "VolumeVO"
            delegate.resourceUuid = volume.uuid
            delegate.trashType = "MigrateVolume"
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assertTrash(trashs)
    }

    void assertTrash(List<org.zstack.sdk.InstallPathRecycleInventory> trashs) {
        assert trashs.get(0).size == volume.size
        assert trashs.get(0).installPath == volume.installPath
        assert trashs.get(0).storageUuid == ps.uuid
        assert trashs.get(0).storageType == "PrimaryStorageVO"
        assert trashs.get(0).resourceType == "VolumeVO"
        assert trashs.get(0).resourceUuid == volume.uuid
        assert trashs.get(0).trashType == "MigrateVolume"
        assert trashs.get(0).trashId > 0
    }

    void testDeleteTrashSingle() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>
        def count = trashs.size()

        cleanUpTrashOnPrimaryStorage {
            uuid = ps.uuid
            trashId = label.trashId
        }

        trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() == count - 1
    }

    void testCleanUpTrash() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>
        def count = trashs.size()

        def result = cleanUpTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as CleanUpTrashOnPrimaryStorageResult

        assert result.result != null
        assert result.result.size == volume.size * count
        assert result.result.resourceUuids.size() == count

        trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() == 0
    }

    // if installpath still in using (this situation usually caused by bug), then skip delete it
    void testCleanUpTrashWhileUsing() {
        def trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() > 0
        def count = trashs.size()

        def action = new CleanUpTrashOnPrimaryStorageAction()
        action.uuid = ps.uuid
        action.sessionId = adminSession()

        def result = action.call()

        assert result.error == null

        trashs = getTrashOnPrimaryStorage {
            uuid = ps.uuid
        } as List<org.zstack.sdk.InstallPathRecycleInventory>

        assert trashs.size() == count
    }
}
