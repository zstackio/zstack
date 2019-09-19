package org.zstack.test.integration.core.trash

import org.zstack.core.trash.StorageRecycleImpl
import org.zstack.core.trash.TrashType
import org.zstack.sdk.CleanUpTrashOnPrimaryStorageAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2019/6/12.*/
class CephReImageTrashCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    StorageRecycleImpl trashMrg

    VolumeInventory volume
    VmInstanceInventory vm
    VolumeSnapshotInventory snapshot1
    VolumeSnapshotInventory snapshot2


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

            snapshot1 = createVolumeSnapshot {
                name = "snapshot1"
                volumeUuid = vm.rootVolumeUuid
            } as VolumeSnapshotInventory

            stopVmInstance {
                uuid = vm.uuid
            }

            assert trashMrg.getTrashList(ps.uuid).isEmpty()
            vm = reimageVmInstance {
                vmInstanceUuid = vm.uuid
            } as VmInstanceInventory
            def afterReimageTrashes = trashMrg.getTrashList(ps.uuid)
            assert afterReimageTrashes.size() == 1
            assert afterReimageTrashes.get(0).trashType == TrashType.ReimageVolume.toString()

            snapshot2 = createVolumeSnapshot {
                name = "snapshot2"
                volumeUuid = vm.rootVolumeUuid
            } as VolumeSnapshotInventory

            revertVolumeFromSnapshot {
                uuid = snapshot1.uuid
            }

            // revert snapshot will restore before trash if need, and create new trash
            def afterRevertTrashes = trashMrg.getTrashList(ps.uuid)
            assert afterRevertTrashes.size() == 1
            assert afterRevertTrashes.get(0).trashType == TrashType.RevertVolume.toString()

            testCheckTrashBeforeDeleteSnapshot()

            deleteVolumeSnapshot {
                uuid = snapshot2.uuid
            }

            testCheckTrashAfterDeleteSnapshot()
        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-ps") as PrimaryStorageInventory
        vm = env.inventoryByName("vm") as VmInstanceInventory
        trashMrg = bean(StorageRecycleImpl.class)
    }

    void testCheckTrashBeforeDeleteSnapshot() {
        def count = trashMrg.getTrashList(ps.uuid).size()
        assert count > 0

        def action = new CleanUpTrashOnPrimaryStorageAction()
        action.uuid = ps.uuid
        action.sessionId = adminSession()
        def result = action.call()

        assert result.error != null

        assert trashMrg.getTrashList(ps.uuid).size() == count
    }

    void testCheckTrashAfterDeleteSnapshot() {
        def count = trashMrg.getTrashList(ps.uuid).size()
        assert count > 0

        def action = new CleanUpTrashOnPrimaryStorageAction()
        action.uuid = ps.uuid
        action.sessionId = adminSession()
        def result = action.call()

        assert result.error == null

        assert trashMrg.getTrashList(ps.uuid).size() == count - 1
    }
}
