package org.zstack.test.integration.storage.snapshot

import org.zstack.core.db.Q
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Cover ZSTAC-76704 / TIC-5745:
 *
 * APIUndoSnapshotCreationMsg: after blockCommit succeeds on the data plane,
 * a failure when deleting the origin volume bits must NOT roll back the
 * control plane. Before the fix, MarkSnapshotAsVolume ran AFTER the
 * delete-origin-volume-bits flow, so a delete failure aborted the undo and
 * left the management-plane install path pointing at the origin path that
 * had already been committed away on the data plane (control/data plane
 * inconsistency).
 *
 * The fix re-orders the flows so MarkSnapshotAsVolume (update-db-install-path)
 * runs BEFORE delete-origin-volume-bits, and a delete failure only submits a
 * PrimaryStorageDeleteBitGC job instead of failing the whole undo.
 *
 * This case injects a delete-bits failure during undoSnapshotCreation and
 * verifies:
 *   1. the undo API still succeeds despite the delete-bits failure;
 *   2. the volume install path is updated to the new (committed) path;
 *   3. MarkSnapshotAsVolume ran BEFORE the delete-bits call (ordering);
 *   4. the failed delete only submits a PrimaryStorageDeleteBitGC job.
 *
 * Note: the delete-bits failure is injected as a VOLUME_IN_USE error rather
 * than a raw HTTP error, because local storage swallows raw transport errors
 * with its own internal GC and still replies success - a VOLUME_IN_USE reply
 * is the way to make DeleteVolumeBitsOnPrimaryStorageMsg actually fail back
 * to the undo flow, which is the path the fix changes.
 */
class UndoSnapshotCreationDeleteBitsFailureCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testDeleteBitsFailureStillUndoSnapshot()
        }
    }

    void testDeleteBitsFailureStillUndoSnapshot() {
        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "snapshot-for-undo"
        } as VolumeSnapshotInventory

        VolumeInventory rootBefore = queryVolume {
            conditions = ["uuid=${vm.rootVolumeUuid}"]
        }[0] as VolumeInventory
        String originPath = rootBefore.installPath
        assert originPath != snapshot.primaryStorageInstallPath

        // install path observed inside the delete-bits simulator: proves
        // whether MarkSnapshotAsVolume already ran by the time delete-bits
        // is reached (ordering assertion).
        String installPathWhenDeleteHit = null
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { LocalStorageKvmBackend.DeleteBitsRsp rsp ->
            installPathWhenDeleteHit = Q.New(VolumeVO.class)
                    .select(VolumeVO_.installPath)
                    .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                    .findValue()
            // make DeleteVolumeBitsOnPrimaryStorageMsg fail back to the undo
            // flow (local storage does not run its internal GC for in-use).
            rsp.setError("volume in use - on purpose - ZSTAC-76704")
            rsp.inUse = true
            return rsp
        }

        // before the fix the failed delete-bits would fail the whole undo;
        // after the fix this API must still succeed.
        undoSnapshotCreation {
            uuid = vm.rootVolumeUuid
            snapShotUuid = snapshot.uuid
        }

        env.cleanSimulatorAndMessageHandlers()

        VolumeInventory rootAfter = queryVolume {
            conditions = ["uuid=${vm.rootVolumeUuid}"]
        }[0] as VolumeInventory

        // 1. control plane committed: volume install path updated to the new path
        assert rootAfter.installPath != originPath
        assert rootAfter.installPath == snapshot.primaryStorageInstallPath

        // 2. ordering: MarkSnapshotAsVolume ran BEFORE delete-origin-volume-bits,
        //    so the DB install path was already the new path when delete was hit.
        assert installPathWhenDeleteHit != null
        assert installPathWhenDeleteHit == snapshot.primaryStorageInstallPath

        // 3. the failed delete only submits a PrimaryStorageDeleteBitGC job;
        //    the undo itself still succeeds.
        retryInSecs {
            def gcs = queryGCJob {
                conditions = ["context~=%${vm.rootVolumeUuid}%".toString()]
            } as List<GarbageCollectorInventory>
            GarbageCollectorInventory gc = gcs.find {
                it.name.contains("gc-delete-bits-volume-${vm.rootVolumeUuid}")
            } as GarbageCollectorInventory
            assert gc != null : "expected PrimaryStorageDeleteBitGC submitted, got: ${gcs.collect { it.name }}"
        }
    }
}
