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
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.header.core.trash.InstallPathRecycleVO
import org.zstack.header.core.trash.InstallPathRecycleVO_
import org.zstack.header.storage.primary.CleanUpTrashOnPrimaryStroageMsg

class DeleteFromDbVolumeTrashCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    DiskOfferingInventory diskOffering
    ClusterInventory cluster
    StorageRecycleImpl trashMrg
    def invoked = false

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
            def cleanup = notifyWhenReceivedMessage(CleanUpTrashOnPrimaryStroageMsg.class) { CleanUpTrashOnPrimaryStroageMsg msg ->
                invoked = true
            }

            testDeleteVolumeDirectWillDeleteTrashFromDB()
            testDeleteVolumeWithDbOnlyWillDeleteTrashFromDB()

            cleanup()
        }
    }

    void testDeleteVolumeWithDbOnlyWillDeleteTrashFromDB() {
        assert Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volume.uuid).isExists()

        cluster = env.inventoryByName("cluster")

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        deletePrimaryStorage {
            uuid = ps.uuid
        }

        assert !Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volume.uuid).isExists()

        assert !invoked
    }

    void testDeleteVolumeDirectWillDeleteTrashFromDB() {
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

        def volumeToDelete = createDataVolume {
            name = "volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        def vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeToDelete.uuid).find() as VolumeVO
        def v = org.zstack.header.volume.VolumeInventory.valueOf(vo)
        InstallPathRecycleInventory tmpTrash = trashMrg.createTrash(TrashType.MigrateVolume, true, v) as InstallPathRecycleInventory

        vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).find() as VolumeVO
        v = org.zstack.header.volume.VolumeInventory.valueOf(vo)
        InstallPathRecycleInventory remainingTrash = trashMrg.createTrash(TrashType.MigrateVolume, true, v) as InstallPathRecycleInventory

        assert Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volumeToDelete.uuid).isExists()
        assert Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volume.uuid).isExists()

        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue("Direct")

        deleteDataVolume {
            uuid = volumeToDelete.uuid
        }

        assert !Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volumeToDelete.uuid).isExists()
        assert Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volume.uuid).isExists()

        assert !invoked
    }
}

